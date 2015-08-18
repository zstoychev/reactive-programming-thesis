package functional.expression_problem.fp

sealed abstract class Figure
case class Circle(radius: Double) extends Figure
case class Square(side: Double) extends Figure

// Добавянето на нов тип е трудно (добавя се към всяка операция и изисква прекомпилация на операциите):
case class Rectangle(a: Double, b: Double) extends Figure
