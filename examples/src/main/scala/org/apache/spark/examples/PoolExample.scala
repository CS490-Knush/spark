package org.apache.spark.examples

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * An example of running
  * spark-submit --class com.supergloo.PoolExample
  * --conf spark.scheduler.mode=FAIR
  * --conf spark.scheduler.allocation.file=./src/main/resources/fair-example.xml
  * --master spark://tmcgrath-rmbp15.local:7077
  * ./target/scala-2.11/spark-2-assembly-1.0.jar
  */
 // spark-submit --class com.supergloo.PoolExample --master spark://tmcgrath-rmbp15.local:7077 ./target/scala-2.11/spark-2-assembly-1.0.jar

object PoolExample {

  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("Pool Example After")
    // conf.setIfMissing("spark.master", "local[*]")

    val spark = SparkSession
      .builder()
      .config(conf)
      .getOrCreate()

    // before FAIR pool
   Range(0, 15).foreach { i =>
     val csv = spark.read
       .csv(s"file:////home/anushreeagrawal/spark/csv_file.csv")
     println(csv.count)
     csv.foreachPartition(i => println(i))
   }

    // After FAIR POOL
    // spark.sparkContext.setLocalProperty("spark.scheduler.pool", "fair_pool")

    // Range(0, 15).par.foreach { i =>
    //   val csv = spark.read
    //     .csv(s"file://///Users/anushreeagrawal/yhack-website/2017-website/acceptances_r1.csv")
    //   println(csv.count)

    //   spark.sparkContext.setLocalProperty("spark.scheduler.pool", "a_different_pool")
    //   csv.foreachPartition(i => println(i))
    // }
    System.in.read();
    spark.stop()
    sys.exit(0)
  }
}
