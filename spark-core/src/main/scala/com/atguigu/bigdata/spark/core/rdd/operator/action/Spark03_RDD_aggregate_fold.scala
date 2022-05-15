package com.atguigu.bigdata.spark.core.rdd.operator.action

import org.apache.spark.{SparkConf, SparkContext}

object Spark03_RDD_aggregate_fold {

    def main(args: Array[String]): Unit = {

        val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("Operator")
        val sc = new SparkContext(sparkConf)

        val rdd = sc.makeRDD(List(1,2,3,4),2)

        // TODO - 行动算子

        //10 + (10 + 1 + 2) + (10 + 3 + 4) = 40
        // aggregateByKey : 初始值只会参与分区内计算
        // aggregate : 初始值会参与分区内计算,并且和参与分区间计算
        val aggregateResult = rdd.aggregate(10)(_+_, _+_)
        println("aggregateResult = " + aggregateResult)
        // fold : 分区内和分区间计算规则一致时可以使用fold
        val foldResult: Int = rdd.fold(10)(_+_)
        println("foldResult = " + foldResult)

        sc.stop()

    }
}
