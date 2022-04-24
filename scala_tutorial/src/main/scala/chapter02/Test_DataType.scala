package chapter02

object Test_DataType {
  def main(args: Array[String]): Unit = {
    val a1: Byte = 127
    val a2: Byte = -128
    // val a2: Byte = 128 // error

    val a3 = 12 // 整数默认类型为Int
    val a4: Long = 123546759887L  // 长整型数据定义

    val b1: Byte = 10
    val b2: Byte = (10+20)
    println("b2 = " + b2);

    val b3: Byte = (b1+20).toByte
    println(b3)

    // 布尔类型
    val flag: Boolean = true
    println(flag)

    
  }

}
