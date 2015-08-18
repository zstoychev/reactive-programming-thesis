package functional.expression_problem

import scala.math.Pi

package object fp {
  def area(figure: Figure) = figure match {
    case Circle(radius) => Pi * radius * radius
    case Square(side) => side * side

    // Добавянето на нов тип е трудно (добавя се към всяка операция и изисква прекомпилация на операциите):
    case Rectangle(a, b) => a * b
  }

  // Добавянето на нова операция е лесно (добавя се само на едно място и не изисква прекомпилация на същестеуващия код):
  def circumference(figure: Figure) = figure match {
    case Circle(radius) => 2 * Pi * radius
    case Square(side) => 4 * side

    // Добавянето на нов тип е трудно (добавя се към всяка операция и изисква прекомпилация на операциите):
    case Rectangle(a, b) => 2 * (a + b)
  }
}
