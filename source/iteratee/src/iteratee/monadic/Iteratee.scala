package iteratee.monadic

import functional.Monad
import iteratee.simple

import scala.language.higherKinds

trait Iteratee[I, O, F[_]] {
  def value: F[Step[I, O, F]]
}

object Iteratee {
  def apply[I, O, F[_]](step: Step[I, O, F])(implicit m: Monad[F]) = new Iteratee[I, O, F] {
    def value: F[Step[I, O, F]] = (m.unit(step))
  }

  def liftSimpleIteratee[I, O, F[_]](iteratee: simple.Iteratee[I, O])
                                    (implicit m: Monad[F]): Iteratee[I, O, F] = iteratee match {
    case simple.Done(value, rest) => Iteratee(Done(value, rest))
    case simple.Error(e) => Iteratee(Error(e))
    case simple.Cont(k) => Iteratee(Cont(input => liftSimpleIteratee(k(input))))
  }
}
