package functional.expression_problem.oop

// Добавянето на нов тип е лесно (добавя се само на едно отделно място и не изисква прекомпилация на съществуващия код):
case class Rectangle(a: Double, b: Double) extends Figure{
  def area: Double = a * b

  // Добавянето на нова операция е трудно (добавяне се към всеки тип и изисква прекомпилация):
  def circumference: Double = 2 * (a + b)
}
