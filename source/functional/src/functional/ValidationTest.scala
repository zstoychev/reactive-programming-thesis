package functional

import functional.util.{Invalid, Valid, Validation}

object ValidationTest {
  type V[T] = Validation[String, T]

  case class Person(firstName: String, lastName: String, age: Int)

  def validateName(name: String): V[String] = {
    if (name.length > 2) Valid(name) else Invalid("Name too short " + name)
  }

  def validateAge(age: Int): V[Int] = {
    if (age >= 18) Valid(age) else Invalid("You must be over 18")
  }

  def person(firstName: String, lastName: String, age: Int): V[Person] = {
    Applicative.map3(
      validateName(firstName),
      validateName(lastName),
      validateAge(age)
    )(Person.apply _)
  }

  def main(args: Array[String]) {
    println(person("Zdravko", "Stoychev", 27))
    println(person("Z", "Stoychev", 17))
  }
}
