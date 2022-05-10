package com.atguigu.bigdata.spark.core.wc

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object WC01_WordCount {
  def main(args: Array[String]): Unit = {

    val sparkConf: SparkConf = new SparkConf().setMaster("local").setAppName("WordCount")
    val sc: SparkContext = new SparkContext(sparkConf)

    val lines: RDD[String] = sc.textFile("datas")
    val wordMap: RDD[String] = lines.flatMap(lines => lines.split(" "))
    //wordMap.map(word=>(word,1))
    val value: RDD[(String, Int)] = wordMap.map((_, 1))

    value.foreach(println(_))
    println(value.toString())
  }
}
