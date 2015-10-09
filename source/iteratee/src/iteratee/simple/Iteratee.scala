package iteratee.simple

import functional.{Monad, MonadWithZero}
import functional.util.{Failure, Success, Try}
import iteratee.{EOF, Empty, El, Input}

sealed trait Iteratee[I, +O] {
  def end = this match {
    case Cont(k) => k(EOF)
    case i => i
  }

  def run: Try[O] = this.end match {
    case Done(value, _) => Success(value)
    case Error(error) => Failure(error)
    case _ => Failure(new IllegalStateException("Diverging iteratee"))
  }

  def map[B](f: O => B): Iteratee[I, B] = this match {
    case Done(value, rest) => Done(f(value), rest)
    case Cont(k) => Cont(i => k(i) map f)
    case Error(e) => Error(e)
  }

  def filter(f: O => Boolean): Iteratee[I, O] = this match {
    case done@Done(value, rest) =>
      if (f(value)) done
      else Error(new NoSuchElementException)
    case it => it
  }

  def withFilter(f: O => Boolean) = filter(f)

  def flatMap[B](f: O => Iteratee[I, B]): Iteratee[I, B] = this match {
    case Done(value, rest) => f(value) match {
      case Done(_, El(_)) => Error(new IllegalStateException("Invalid initial iteratee state"))
      case Done(valueB, _) => Done(valueB, rest)
      case Cont(k) => k(rest)
      case Error(e) => Error(e)
    }
    case Cont(k) => Cont(i => k(i) flatMap f)
    case Error(e) => Error(e)
  }
}

case class Done[I, +O](value: O, rest: Input[I]) extends Iteratee[I, O]
case class Error[I, +O](error: Throwable) extends Iteratee[I, O]
case class Cont[I, +O](k: Input[I] => Iteratee[I, O]) extends Iteratee[I, O]

object Iteratee {
  implicit def iterateeMonad[I] = new MonadWithZero[({type f[+O] = Iteratee[I, O]})#f] {
    def mzero[A]: Iteratee[I, A] = Error(new NoSuchElementException)
    def flatMap[A, B](m: Iteratee[I, A])(f: (A) => Iteratee[I, B]): Iteratee[I, B] = m flatMap f
    def unit[A](a: => A): Iteratee[I, A] = Done(a, Empty)

    override def map[A, B](m: Iteratee[I, A])(f: (A) => B): Iteratee[I, B] = m map f
  }

  def head[A] = {
    def step: Input[A] => Iteratee[A, Option[A]] = {
      case El(e) => Done(Some(e), Empty)
      case Empty => Cont(step)
      case EOF => Done(None, EOF)
    }
    Cont(step)
  }

  def fold[I, O](initial: O)(f: (O, I) => O): Iteratee[I, O] = {
    def step(state: O): Input[I] => Iteratee[I, O] = {
      case El(e) => Cont(step(f(state, e)))
      case Empty => Cont(step(state))
      case EOF => Done(state, EOF)
    }
    Cont(step(initial))
  }

  val sum = fold[Int, Int](0)(_ + _)

  def take[A](n: Int): Iteratee[A, List[A]] = {
    def step(elements: List[A]): Input[A] => Iteratee[A, List[A]] = {
      case El(e) if elements.size == n - 1 => Done((e :: elements).reverse, Empty)
      case El(e) => Cont(step(e :: elements))
      case Empty => Cont(step(elements))
      case EOF => Done(elements.reverse, EOF)
    }
    Cont(step(Nil))
  }

  def drop[I](n: Int): Iteratee[I, Unit] = {
    if (n == 0) Done((), Empty)
    else Cont {
      case El(_) => drop(n - 1)
      case Empty => drop(n)
      case EOF => Done((), EOF)
    }
  }

  def foreach[I](f: I => Unit): Iteratee[I, Unit] = {
    def step: Input[I] => Iteratee[I, Unit] = {
      case El(e) => f(e); Cont(step)
      case Empty => Cont(step)
      case EOF => Done((), EOF)
    }
    Cont(step)
  }

  val line: Iteratee[Char, String] = {
    def step(line: String): Input[Char] => Iteratee[Char, String] = {
      case Empty => Cont(step(line))
      case EOF => Done(line, EOF)
      case El('\n') => Done(line, Empty)
      case El(c) => Cont(step(line + c))
    }
    Cont(step(""))
  }.map(_.stripSuffix("\r"))

  val lineChunked: Iteratee[String, String] = {
    def step(chunks: List[String]): Input[String] => Iteratee[String, String] = {
      case Empty => Cont(step(chunks))
      case EOF => Done(chunks.reverse.mkString, EOF)
      case El(chars) => chars.span(_ != '\n') match {
        case (_, "") => Cont(step(chars :: chunks))
        case (lineEnd, rest) => Done((lineEnd :: chunks).reverse.mkString, El(rest.tail))
      }
    }
    Cont(step(List.empty))
  }.map(_.stripSuffix("\r"))

  val asciiLineChunked: Iteratee[Array[Byte], String] = {
    def toString(chunks: List[Array[Byte]]) = new String(Array.concat(chunks.reverse: _*), "US-ASCII")

    def step(chunks: List[Array[Byte]]): Input[Array[Byte]] => Iteratee[Array[Byte], String] = {
      case Empty => Cont(step(chunks))
      case EOF => Done(toString(chunks), EOF)
      case El(chars) => chars.span(_ != '\n') match {
        case (_, Array()) => Cont(step(chars :: chunks))
        case (lineEnd, rest) => Done(toString(lineEnd :: chunks), El(rest.tail))
      }
    }
    Cont(step(List.empty))
  }.map(_.stripSuffix("\r"))
}
