package com.atguigu.bigdata.spark.core.rdd.operator.action

import org.apache.spark.{SparkConf, SparkContext}

object Spark04_RDD_countByKey {

    def main(args: Array[String]): Unit = {

        val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("Operator")
        val sc = new SparkContext(sparkConf)
        // TODO - 行动算子
        val rdd2 = sc.makeRDD(List(1,1,1,99,99,8888),2)
        val intToLong: collection.Map[Int, Long] = rdd2.countByValue()
        println(intToLong)

        val rdd = sc.makeRDD(List(
          ("a", 1),("a", 2),("a", 3)
        ))
        // 统计每种 key 的个数
        val stringToLong: collection.Map[String, Long] = rdd.countByKey()
        println(stringToLong)

        sc.stop()

    }
}
