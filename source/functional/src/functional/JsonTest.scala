package functional

import functional.json._

object JsonTest {
  case class Person(firstName: String, lastName: String, age: Int, nickname: Option[String])

  object Person {
    implicit val personJsonSerializable = new JsonSerializable[Person] {
      def toJson(person: Person) = JsonObject(Map(
        "firstName" -> JsonString(person.firstName),
        "lastName" -> JsonString(person.lastName),
        "age" -> JsonNumber(person.age),
        "nickname" -> person.nickname.map(JsonString).getOrElse(JsonNull)
      ))
    }
  }

  def main(args: Array[String]) {
    println(Person("Ivan", "Georgiev", 25, None).toJsonString)

    println(List(
      Person("Ivan", "Georgiev", 25, None),
      Person("Georgi", "Ivanov", 19, None)
    ).toJson)
  }
}
