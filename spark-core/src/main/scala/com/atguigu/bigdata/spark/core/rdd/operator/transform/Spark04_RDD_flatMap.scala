package com.atguigu.bigdata.spark.core.rdd.operator.transform

import org.apache.calcite.linq4j.tree.Expressions.list
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object Spark04_RDD_flatMap {

    def main(args: Array[String]): Unit = {

        val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("Operator")
        val sc = new SparkContext(sparkConf)

        // TODO 算子 - mapPartitions
        val rdd: RDD[List[Int]] = sc.makeRDD(List(
            List(1, 2), List(3, 4)
        ))
        val flatRDD: RDD[Int] = rdd.flatMap(
            list => {
                list
            }
        )
        flatRDD.collect().foreach(println)



        sc.stop()

    }
}
