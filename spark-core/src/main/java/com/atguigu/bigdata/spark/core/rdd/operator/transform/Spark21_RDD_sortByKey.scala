package com.atguigu.bigdata.spark.core.rdd.operator.transform

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object Spark21_RDD_sortByKey {

    def main(args: Array[String]): Unit = {

        val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("Operator")
        val sc = new SparkContext(sparkConf)

        // TODO 算子 - (Key - Value类型)

        val rdd: RDD[(String, Int)] = sc.makeRDD(List(
            ("a", 2), ("a", 1), ("c", 9), ("b",50)
        ))

        val value: RDD[(String, Int)] = rdd.sortByKey(true)
        value.collect().foreach(println)

        println("--------------------------------")
        val value2: RDD[(String, Int)] = rdd.sortByKey(true)
        value2.collect().foreach(println)
        sc.stop()

    }
}
