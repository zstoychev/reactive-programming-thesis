package functional.util

case class Default[+T](value: T)

object Default {
  implicit val booleanDefault = Default(0)
  implicit val intDefault = Default(0)
  implicit val doubleDefault = Default(0.0)
  implicit val stringDefault = Default("")
  implicit val throwableDefault = Default(new NoSuchElementException)
  implicit val listDefault = Default(Nil)
}
