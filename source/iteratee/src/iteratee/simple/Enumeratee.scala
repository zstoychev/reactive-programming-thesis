package iteratee.simple

import iteratee.{EOF, Empty, El, Input}

import scala.annotation.tailrec

trait Enumeratee[U, V] {
  def apply[O](iteratee: Iteratee[V, O]): Iteratee[U, Iteratee[V, O]]

  def transform[O](iteratee: Iteratee[V, O]): Iteratee[U, O] = this(iteratee) flatMap {
    case Done(value, _) => Done(value, Empty)
    case Error(e) => Error(e)
    case Cont(k) => k(EOF) match {
      case Done(value, _) => Done(value, Empty)
      case Error(e) => Error(e)
      case Cont(k) => Error(new IllegalStateException("Diverging iteratee"))
    }
  }

  def &>>[O](iteratee: Iteratee[V, O]) = transform(iteratee)
}

object Enumeratee {
  def map[A, B](f: A => B) = new Enumeratee[A, B] {
    def apply[O](iteratee: Iteratee[B, O]): Iteratee[A, Iteratee[B, O]] = iteratee match {
      case i@Done(_, _) => Done(i, Empty)
      case Error(e) => Error(e)
      case Cont(k) =>
        def step: Input[A] => Iteratee[A, Iteratee[B, O]] = {
          case El(value) => apply(k(El(f(value))))
          case Empty => Cont(step)
          case EOF => Done(k(Empty), EOF)
        }
        Cont(step)
    }
  }

  def filter[A](f: A => Boolean) = new Enumeratee[A, A] {
    def apply[O](iteratee: Iteratee[A, O]): Iteratee[A, Iteratee[A, O]] = iteratee match {
      case i@Done(_, _) => Done(i, Empty)
      case Error(e) => Error(e)
      case Cont(k) =>
        def step: Input[A] => Iteratee[A, Iteratee[A, O]] = {
          case el@El(value) if f(value) => apply(k(el))
          case EOF => Done(k(Empty), EOF)
          case _ => Cont(step)
        }
        Cont(step)
    }
  }

  def grouped[U, V](generator: Iteratee[U, V]) = new Enumeratee[U, V] {
    def apply[O](iteratee: Iteratee[V, O]): Iteratee[U, Iteratee[V, O]] = iteratee match {
      case i@Done(_, _) => Done(i, Empty)
      case Error(e) => Error(e)
      case Cont(k) =>
        def step(genState: Iteratee[U, V]): Input[U] => Iteratee[U, Iteratee[V, O]] = {
          case Empty => Cont(step(genState))
          case EOF => genState.end match {
            case Done(value, _) => Done(k(El(value)), EOF)
            case Error(e) => Error(e)
            case _ => Error(new IllegalStateException(("Diverging iteratee")))
          }
          case el@El(value) => Enumerator(value)(genState) match {
            case Done(value, El(restEl)) => apply(k(El(value))) match {
              case Done(i, Empty) => Done(i, El(restEl))
              case it => Enumerator(restEl)(it) // can lead to stack overflow
            }
            case Done(value, _) => apply(k(El(value)))
            case Error(e) => Error(e)
            case c@Cont(_) => Cont(step(c))
          }
        }
        Cont(step(generator))
    }
  }

  val asciiDecoder: Enumeratee[Array[Byte], String] = Enumeratee.map(new String(_, "US-ASCII"))
  val asciiLines = Enumeratee.grouped(Iteratee.asciiLineChunked)
  val lines = Enumeratee.grouped(Iteratee.lineChunked)
}
