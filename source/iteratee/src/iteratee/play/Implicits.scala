package iteratee.play

import functional.MonadWithZero
import play.api.libs.iteratee.Input.Empty
import play.api.libs.iteratee.{Done, Enumerator, Error, Iteratee}

import scala.concurrent.ExecutionContext.Implicits.global

object Implicits {
  implicit def iterateeMonad[I] = new MonadWithZero[({type f[+O] = Iteratee[I, O]})#f] {
    def mzero[A]: Iteratee[I, A] = Error("No such element", Empty)
    def flatMap[A, B](m: Iteratee[I, A])(f: (A) => Iteratee[I, B]): Iteratee[I, B] = m flatMap f
    def unit[A](a: => A): Iteratee[I, A] = Done(a, Empty)

    override def map[A, B](m: Iteratee[I, A])(f: (A) => B): Iteratee[I, B] = m map f
  }

  implicit val enumeratorMonad = new MonadWithZero[Enumerator] {
    def mzero[A]: Enumerator[A] = Enumerator()
    def flatMap[A, B](m: Enumerator[A])(f: (A) => Enumerator[B]): Enumerator[B] = m flatMap f
    def unit[A](a: => A): Enumerator[A] = Enumerator(a)

    override def map[A, B](m: Enumerator[A])(f: (A) => B): Enumerator[B] = m map f
  }
}
