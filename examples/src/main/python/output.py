#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from __future__ import print_function

import sys
from operator import add

from pyspark.sql import SparkSession
from pyspark import SparkContext
# import SparkContext as sc


if __name__ == "__main__":
    # if len(sys.argv) != 2:
    #     print("Usage: wordcount <file>", file=sys.stderr)
    #     exit(-1)

    # spark = SparkSession\
    #     .builder\
    #     .appName("PythonWordCount")\
    #     .getOrCreate()

    sc = SparkContext("spark://instance-2.us-east1-b.c.cpsc490-222104.internal:7077", "First App")

    # data = [1, 2, 3, 4, 5]
    # distData = sc.parallelize(data)

    # distData.persist()

    lines = sc.textFile("examples/src/main/python/input.txt", 2)
    # lines_par = sc.parallelize(lines)

    lines.setName("lines")
    lines.cache()
    lines.collect()

    # lines = spark.read.text("examples/src/main/python/input.txt")
    lines_mapped = lines.map(lambda r: r[0])
    # lines_par = sc.parallelize(lines_mapped)
    counts = lines_mapped.flatMap(lambda x: x.split(' ')) \
                  .map(lambda x: (x, 1)) \
                  .reduceByKey(add)

    counts.setName("counts")
    counts.cache()

    output = counts.collect()
    for (word, count) in output:
        print("%s: %i" % (word, count))
    lines.persist()
    print("done")

    myrdd = sc.parallelize(range(0,100))
    myrdd.setName("test")
    myrdd.cache()
    myrdd.collect()

    input()
    # counts = text_file.flatMap(lambda line: line.split(" ")) \
    #          .map(lambda word: (word, 1)) \
    #          .reduceByKey(lambda a, b: a + b)

    # output = counts.collect()
    # for (word, count) in output:
    #     print("%s: %i" % (word, count))
    # counts.saveAsTextFile("hdfs://...")

    # lines = spark.read.text(sys.argv[1]).rdd.map(lambda r: r[0])
    # counts = lines.flatMap(lambda x: x.split(' ')) \
    #               .map(lambda x: (x, 1)) \
    #               .reduceByKey(add)
    # output = counts.collect()
    # for (word, count) in output:
    #     print("%s: %i" % (word, count))

    # spark.stop()
