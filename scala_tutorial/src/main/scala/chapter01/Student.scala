package chapter01

class Student(var name: String,var age:Int){

    def printInfo(): Unit = {
      println(name + " " + age + " " + Student.school)
    }
}

// 引入伴生对象
object Student{
  var  school: String = "怪兽大学";

  def main(args: Array[String]): Unit = {
    val bob = new Student("Bob", 21)
    val albert = new Student("Albert", 21)
    val alice = new Student("Alice", 74)

    alice.printInfo()
    bob.printInfo()
    albert.printInfo()
  }
}
