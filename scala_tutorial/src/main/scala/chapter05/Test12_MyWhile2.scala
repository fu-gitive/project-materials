package chapter05

object Test12_MyWhile2 {
  def main(args: Array[String]): Unit = {
    var n = 10;

    //1. 常规的While循序
    while (n >= 1) {
      println(n)
      n -= 1
    }

    println("==========================1")

    // 用闭包实现一个函数，将代码块作为参数传入，递归调用
    def testWhile(condition: Boolean): (=> Unit) => Unit = {
      def doLoop(op: => Unit): Unit = {
        if (condition) {
          op
          testWhile(condition)(op)
        }
      }
      doLoop _
    }
  }
}
