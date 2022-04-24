package chapter02

import scala.io.StdIn

object Test02_Stdin {

  def main(args: Array[String]): Unit = {
    println("please type your name: ")
    val name: String = StdIn.readLine()
    println("please type your age: ")
    val age: Int = StdIn.readInt()

    //控制台打印输出
    println(s"welcome to join the Scala world ${age} 岁的 ${name}。")
  }
}
