package chapter02

import java.io.{File, PrintWriter}
import scala.io.Source

object FileIO {
  def main(args: Array[String]): Unit = {
    //1. 从文件读取内容
    Source.fromFile("src/main/resources/test.txt").foreach(print)

    //2. 将数据写入文件
    val writer = new PrintWriter(new File("src/main/resources/output.txt"))
    writer.write("Scala test output")
    writer.close()
  }
}
