package functional.util

import functional.MonadWithZero

// Имплементацията игнорира хвърлянето на изключения

sealed trait Try[+T] {
  def get: T
  def isSuccess: Boolean
  def isFailure: Boolean = !isSuccess

  def getOrElse[V >: T](default: => V): V
  def orElse[V >: T](default: => Try[V]): Try[V]

  def map[V](f: T => V): Try[V]
  def flatMap[V](f: T => Try[V]): Try[V]
  def filter(f: T => Boolean): Try[T]
  def withFilter(f: T => Boolean): Try[T] = filter(f)

  def recover[V >: T](f: PartialFunction[Throwable, V]): Try[V]
  def recoverWith[V >: T](f: PartialFunction[Throwable, Try[V]]): Try[V]

  def foreach(f: T => Unit): Unit = if (isSuccess) f(get)
}

object Try {
  implicit val tryMonad = new MonadWithZero[Try] {
    def flatMap[A, B](m: Try[A])(f: A => Try[B]): Try[B] = m flatMap f
    def unit[A](a: => A): Try[A] = Success(a)
    def mzero[A]: Try[A] = Failure(new NoSuchElementException)

    override def map[A, B](m: Try[A])(f: A => B): Try[B] = m map f
  }
}

case class Success[+T](value: T) extends Try[T] {
  def get = value
  def isSuccess = true

  def getOrElse[V >: T](default: => V) = value
  def orElse[V >: T](default: => Try[V]) = this

  def map[V](f: T => V) = Success(f(value))
  def flatMap[V](f: T => Try[V]) = f(value)
  def filter(f: T => Boolean) = if (f(value)) this else Failure(new NoSuchElementException)

  def recover[V >: T](f: PartialFunction[Throwable, V]) = this
  def recoverWith[V >: T](f: PartialFunction[Throwable, Try[V]]) = this
}

case class Failure[+T](exception: Throwable) extends Try[T] {
  def get = throw exception
  def isSuccess = false

  def getOrElse[V >: T](default: => V) = default
  def orElse[V >: T](default: => Try[V]): Try[V] = default

  def map[V](f: T => V) = this.asInstanceOf[Failure[V]]
  def flatMap[V](f: T => Try[V]) = this.asInstanceOf[Failure[V]]
  def filter(f: T => Boolean) = this

  def recover[V >: T](f: PartialFunction[Throwable, V]) = if (f.isDefinedAt(exception)) Success(f(exception)) else this
  def recoverWith[V >: T](f: PartialFunction[Throwable, Try[V]]) = if (f.isDefinedAt(exception)) f(exception) else this
}
