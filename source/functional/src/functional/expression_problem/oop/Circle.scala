package functional.expression_problem.oop

import scala.math.Pi

class Circle(val radius: Double) extends Figure {
  def area: Double = Pi * radius * radius

  // Добавянето на нова операция е трудно (добавяне се към всеки тип и изисква прекомпилация):
  def circumference: Double = 2 * Pi * radius
}
