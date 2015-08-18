package functional

import scala.language.higherKinds

trait Applicative[F[_]] {
  def apply[A, B](mf: F[A => B])(ma: F[A]): F[B]
  def unit[A](a: => A): F[A]


  def map[A, B](m: F[A])(f: A => B): F[B] = map2(m, unit(()))((a, _) => f(a))
  def map2[A, B, C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] = apply(apply(unit(f.curried))(ma))(mb)
  def map3[A, B, C, D](ma: F[A], mb: F[B], mc: F[C])(f: (A, B, C) => D): F[D] =
    apply(apply(apply(unit(f.curried))(ma))(mb))(mc)
  def map4[A, B, C, D, E](ma: F[A], mb: F[B], mc: F[C], md: F[D])(f: (A, B, C, D) => E): F[E] =
    apply(apply(apply(apply(unit(f.curried))(ma))(mb))(mc))(md)

  def traverse[A, B](xs: List[A])(f: A => F[B]): F[List[B]] =
    xs.foldRight(unit(List[B]())) { (next, acc) =>
      map2(f(next), acc)(_ :: _)
    }
  def sequence[A](ml: List[F[A]]): F[List[A]] = traverse(ml)(m => m)
}

object Applicative {
  def map[A, B, F[_]](ma: F[A])(f: A => B)(implicit m: Applicative[F]) = m.map(ma)(f)
  def map2[A, B, C, F[_]](ma: F[A], mb: F[B])(f: (A, B) => C)(implicit m: Applicative[F]) = m.map2(ma, mb)(f)
  def map3[A, B, C, D, F[_]](ma: F[A], mb: F[B], mc: F[C])(f: (A, B, C) => D)(implicit m: Applicative[F]) =
    m.map3(ma, mb, mc)(f)
  def map4[A, B, C, D, E, F[_]](ma: F[A], mb: F[B], mc: F[C], md: F[D])(f: (A, B, C, D) => E)
                               (implicit m: Applicative[F]) =
    m.map4(ma, mb, mc, md)(f)

  def traverse[A, B, F[_]](xs: List[A])(f: A => F[B])(implicit m: Applicative[F]) = m.traverse(xs)(f)
  def sequence[A, F[_]](ml: List[F[A]])(implicit m: Applicative[F]) = m.sequence(ml)
}
