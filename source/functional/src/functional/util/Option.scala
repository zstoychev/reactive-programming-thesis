package functional.util

import functional.MonadWithZero

sealed trait Option[+T] {
  def get: T
  def isEmpty: Boolean

  def getOrElse[V >: T](default: => V): V = if (isEmpty) default else get
  def orElse[V >: T](default: => Option[V]): Option[V] = if (isEmpty) default else this
  def map[V](f: T => V): Option[V] = if (isEmpty) None else Some(f(get))
  def flatMap[V](f: T => Option[V]): Option[V] = if (isEmpty) None else f(get)
  def filter(f: T => Boolean): Option[T] = if (isEmpty || !f(get)) None else this
  def withFilter(f: T => Boolean): Option[T] = filter(f)
  def foreach(f: T => Unit): Unit = if (!isEmpty) f(get)
}

object Option {
  implicit val optionMonad = new MonadWithZero[Option] {
    def flatMap[A, B](m: Option[A])(f: A => Option[B]): Option[B] = m flatMap f
    def unit[A](a: => A): Option[A] = Some(a)
    def mzero[A]: Option[A] = None

    override def map[A, B](m: Option[A])(f: A => B): Option[B] = m map f
  }
}

case class Some[+T](value: T) extends Option[T] {
  def get = value
  def isEmpty = false
}

case object None extends Option[Nothing] {
  def get = throw new NoSuchElementException()
  def isEmpty = true
}
