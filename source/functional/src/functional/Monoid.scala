package functional

trait Monoid[T] {
  def op(a: T, b: T): T
  def identity: T
}

object Monoid {
  def sum[T](xs: List[T])(implicit m: Monoid[T]) = {
    xs.foldLeft(m.identity)((acc, next) => m.op(acc, next))
  }

  implicit val intAdditiveMonoid = new Monoid[Int] {
    def op(a: Int, b: Int): Int = a + b
    def identity: Int = 0
  }

  val intMultiplicativeMonoid = new Monoid[Int] {
    def op(a: Int, b: Int): Int = a * b
    def identity: Int = 1
  }

  implicit val stringMonoid = new Monoid[String] {
    def op(a: String, b: String): String = a + b
    def identity: String = ""
  }
}
