/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.scheduler

import java.nio.ByteBuffer
import java.util.{Locale, Timer, TimerTask}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicLong

import scala.collection.Set
import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}
import scala.util.Random

import org.apache.spark._
import org.apache.spark.TaskState.TaskState
import org.apache.spark.executor.ExecutorMetrics
import org.apache.spark.internal.Logging
import org.apache.spark.internal.config
import org.apache.spark.rpc.RpcEndpoint
import org.apache.spark.scheduler.SchedulingMode.SchedulingMode
import org.apache.spark.scheduler.TaskLocality.TaskLocality
import org.apache.spark.storage.BlockManagerId
import org.apache.spark.util.{AccumulatorV2, ThreadUtils, Utils}

/**
 * Schedules tasks for multiple types of clusters by acting through a SchedulerBackend.
 * It can also work with a local setup by using a `LocalSchedulerBackend` and setting
 * isLocal to true. It handles common logic, like determining a scheduling order across jobs, waking
 * up to launch speculative tasks, etc.
 *
 * Clients should first call initialize() and start(), then submit task sets through the
 * submitTasks method.
 *
 * THREADING: [[SchedulerBackend]]s and task-submitting clients can call this class from multiple
 * threads, so it needs locks in public API methods to maintain its state. In addition, some
 * [[SchedulerBackend]]s synchronize on themselves when they want to send events here, and then
 * acquire a lock on us, so we need to make sure that we don't try to lock the backend while
 * we are holding a lock on ourselves.
 */
private[spark] class ResourceAwareTaskSchedulerImpl(
    sc: SparkContext,
    maxTaskFailures: Int,
    isLocal: Boolean = false)
  extends TaskSchedulerImpl(sc, maxTaskFailures, isLocal) with Logging {

  import ResourceAwareTaskSchedulerImpl._
  logWarning("RESOURCE AWARE TASK")

  def this(sc: SparkContext) = {
    this(sc, sc.conf.get(config.MAX_TASK_FAILURES))
  }
}

private[spark] object ResourceAwareTaskSchedulerImpl {

  val SCHEDULER_MODE_PROPERTY = "spark.scheduler.mode"

  /**
   * Used to balance containers across hosts.
   *
   * Accepts a map of hosts to resource offers for that host, and returns a prioritized list of
   * resource offers representing the order in which the offers should be used. The resource
   * offers are ordered such that we'll allocate one container on each host before allocating a
   * second container on any host, and so on, in order to reduce the damage if a host fails.
   *
   * For example, given {@literal <h1, [o1, o2, o3]>}, {@literal <h2, [o4]>} and
   * {@literal <h3, [o5, o6]>}, returns {@literal [o1, o5, o4, o2, o6, o3]}.
   */
  def prioritizeContainers[K, T] (map: HashMap[K, ArrayBuffer[T]]): List[T] = {
    val _keyList = new ArrayBuffer[K](map.size)
    _keyList ++= map.keys

    // order keyList based on population of value in map
    val keyList = _keyList.sortWith(
      (left, right) => map(left).size > map(right).size
    )

    val retval = new ArrayBuffer[T](keyList.size * 2)
    var index = 0
    var found = true

    while (found) {
      found = false
      for (key <- keyList) {
        val containerList: ArrayBuffer[T] = map.getOrElse(key, null)
        assert(containerList != null)
        // Get the index'th entry for this host - if present
        if (index < containerList.size) {
          retval += containerList.apply(index)
          found = true
        }
      }
      index += 1
    }

    retval.toList
  }

  private def maybeCreateBlacklistTracker(sc: SparkContext): Option[BlacklistTracker] = {
    if (BlacklistTracker.isBlacklistEnabled(sc.conf)) {
      val executorAllocClient: Option[ExecutorAllocationClient] = sc.schedulerBackend match {
        case b: ExecutorAllocationClient => Some(b)
        case _ => None
      }
      Some(new BlacklistTracker(sc, executorAllocClient))
    } else {
      None
    }
  }

}
