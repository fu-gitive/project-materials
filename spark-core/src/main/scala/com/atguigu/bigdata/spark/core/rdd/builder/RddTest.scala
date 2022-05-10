package com.atguigu.bigdata.spark.core.rdd.builder

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object RddTest {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("RDD")
    val sc = new SparkContext(sparkConf)

    val seq: Seq[Int] = Seq[Int](1, 32, 4, 9, 10)
    val rdd: RDD[Int] = sc.parallelize(seq)

    rdd.collect().foreach(println)
    sc.stop()
  }
}
