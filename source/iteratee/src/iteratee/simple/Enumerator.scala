package iteratee.simple

import functional.MonadWithZero
import iteratee.El

import scala.annotation.tailrec

trait Enumerator[I] { self =>
  def apply[O](iteratee: Iteratee[I, O]): Iteratee[I, O]

  def |>>[O](iteratee: Iteratee[I, O]) = apply(iteratee)

  def andThen(other: Enumerator[I]) = new Enumerator[I] {
    def apply[O](iteratee: Iteratee[I, O]): Iteratee[I, O] = other(self(iteratee))
  }

  def map[B](f: I => B) = self through Enumeratee.map(f)

  def flatMap[B](f: I => Enumerator[B]): Enumerator[B] = new Enumerator[B] {
    def apply[O](iteratee: Iteratee[B, O]): Iteratee[B, O] = {
      val selfHandler = Iteratee.fold[I, Iteratee[B, O]](iteratee) { (it, next) => f(next)(it) }

      self(selfHandler).run.get
    }
  }

  def filter(f: I => Boolean) = self through Enumeratee.filter(f)
  def withFilter(f: I => Boolean) = filter(f)

  def through[T](enumeratee: Enumeratee[I, T]): Enumerator[T] = new Enumerator[T] {
    def apply[O](iteratee: Iteratee[T, O]): Iteratee[T, O] = self(enumeratee(iteratee)).end match {
      case Done(i, _) => i
      case Error(e) => Error(e)
      case _ => Error(new IllegalStateException("Unexpected Enumeratee state"))
    }
  }

  def &>[T](enumeratee: Enumeratee[I, T]) = through(enumeratee)
}

object Enumerator {
  implicit val enumeratorMonad = new MonadWithZero[Enumerator] {
    def mzero[A]: Enumerator[A] = Enumerator()
    def flatMap[A, B](m: Enumerator[A])(f: (A) => Enumerator[B]): Enumerator[B] = m flatMap f
    def unit[A](a: => A): Enumerator[A] = Enumerator(a)

    override def map[A, B](m: Enumerator[A])(f: (A) => B): Enumerator[B] = m map f
  }

  def apply[T](values: T*) = new Enumerator[T] {
    def apply[O](iteratee: Iteratee[T, O]): Iteratee[T, O] = values.foldLeft(iteratee) {
      case (Cont(k), next) => k(El(next))
      case (i, _) => i
    }
  }

  def applyThatCuts[T](values: T*) = new Enumerator[T] {
    @tailrec
    private def enumerate[O](values: Seq[T], iteratee: Iteratee[T, O]): Iteratee[T, O] = iteratee match {
      case Cont(k) if !values.isEmpty => enumerate(values.tail, k(El(values.head)))
      case _ => iteratee
    }

    def apply[O](iteratee: Iteratee[T, O]): Iteratee[T, O] = enumerate(values, iteratee)
  }
}
