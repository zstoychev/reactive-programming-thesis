package functional.util

import functional.MonadWithZero

sealed trait Either[+E, +T] {
  def get: T
  def isSuccess: Boolean
  def isFailure: Boolean = !isSuccess

  def getOrElse[V >: T](default: => V): V
  def orElse[F >: E, V >: T](default: => Either[F, V]): Either[F, V]

  def map[V](f: T => V): Either[E, V]
  def flatMap[F >: E, V](f: T => Either[F, V]): Either[F, V]
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]): Either[F, T]
  def withFilter[F >: E](f: T => Boolean)(implicit default: Default[F]): Either[F, T] = filter[F](f)

  def recover[F >: E, V >: T](f: PartialFunction[F, V]): Either[F, V]
  def recoverWith[F >: E, V >: T](f: PartialFunction[F, Either[F, V]]): Either[F, V]

  def foreach(f: T => Unit): Unit = if (isSuccess) f(get)
}

object Either {
  implicit def eitherMonad[E](implicit default: Default[E]) = new MonadWithZero[({type f[T] = Either[E, T]})#f] {
    def flatMap[A, B](m: Either[E, A])(f: A => Either[E, B]): Either[E, B] = m flatMap f
    def unit[A](a: => A): Either[E, A] = Right(a)
    def mzero[A]: Either[E, A] = Left(default.value)

    override def map[A, B](m: Either[E, A])(f: A => B): Either[E, B] = m map f
  }
}

case class Right[+E, +T](value: T) extends Either[E, T] {
  def get = value
  def isSuccess = true

  def getOrElse[V >: T](default: => V) = value
  def orElse[F >: E, V >: T](default: => Either[F, V]) = this

  def map[V](f: T => V) = Right(f(value))
  def flatMap[F >: E, V](f: T => Either[F, V]) = f(value)
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]) = if (f(value)) this else Left(default.value)

  def recover[F >: E, V >: T](f: PartialFunction[F, V]) = this
  def recoverWith[F >: E, V >: T](f: PartialFunction[F, Either[F, V]]) = this
}

case class Left[+E, +T](error: E) extends Either[E, T] {
  def get = throw new NoSuchElementException
  def isSuccess = false

  def getOrElse[V >: T](default: => V) = default
  def orElse[F >: E, V >: T](default: => Either[F, V]) = default

  def map[V](f: T => V) = this.asInstanceOf[Left[E, V]]
  def flatMap[F >: E, V](f: T => Either[F, V]) = this.asInstanceOf[Left[F, V]]
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]) = this

  def recover[F >: E, V >: T](f: PartialFunction[F, V]) = if (f.isDefinedAt(error)) Right(f(error)) else this
  def recoverWith[F >: E, V >: T](f: PartialFunction[F, Either[F, V]]) = if (f.isDefinedAt(error)) f(error) else this
}
