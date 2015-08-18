package functional.expression_problem.oop

abstract class Figure {
  def area: Double

  // Добавянето на нова операция е трудно (добавяне се към всеки тип и изисква прекомпилация):
  def circumference: Double
}
