package chapter06

object MyApp extends APP with Dao {
  def main(args: Array[String]): Unit = {
    login(new User222("Albert", 111))
  }
}


class User222(val name: String, val age: Int)

trait Dao {
  def insert(user: User222)= {
    println("insert into database : " + user.name)
  }
}

trait APP {
  _: Dao =>
  def login(user:User222): Unit = {
    println("login : " + user.name)
    insert(user)
  }
}



