package chapter02

object Test01 {
  def main(args: Array[String]): Unit = {
    //声明一个变量
    var age: Int = 10
    val name: String = "Albert"

    println(s"$age")

    val s1 = s"""
       |select *
       |from
       |  student
       |where
       |  name = ${name}
       |  and age = ${age + 3}
       |""".stripMargin
    println(s1)

    val s2 = s"name=$name da sha bi"
    println(s2)

    val number: Double = 2.34568
    println(f"the number is ${number}%2.2f")  //格式化模板字符串
    println(raw"the number is ${number}%2.2f") // 原样输出
  }
}
