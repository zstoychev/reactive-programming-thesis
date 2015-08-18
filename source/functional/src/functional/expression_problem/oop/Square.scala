package functional.expression_problem.oop

class Square(val side: Double) extends Figure {
  def area: Double = side * side

  // Добавянето на нова операция е трудно (добавяне се към всеки тип и изисква прекомпилация):
  def circumference: Double = 4 * side
}
