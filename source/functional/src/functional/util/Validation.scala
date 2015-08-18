package functional.util

import functional.{Applicative, MonadWithZero}

import scalaz.NonEmptyList

sealed trait Validation[+E, +T] {
  def get: T
  def isValid: Boolean
  def isInvalid: Boolean = !isValid

  def getOrElse[V >: T](default: => V): V
  def orElse[F >: E, V >: T](default: => Validation[F, V]): Validation[F, V]

  def map[V](f: T => V): Validation[E, V]
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]): Validation[F, T]
  def withFilter[F >: E](f: T => Boolean)(implicit default: Default[F]): Validation[F, T] = filter[F](f)

  def foreach(f: T => Unit): Unit = if (isValid) f(get)
}

object Validation {
  implicit def validationApplicative[E](implicit default: Default[E]) =
    new Applicative[({type f[T] = Validation[E, T]})#f] {
      def apply[A, B](mf: Validation[E, A => B])(ma: Validation[E, A]): Validation[E, B] = (mf, ma) match {
        case (Valid(f), Valid(a)) => Valid(f(a))
        case (Invalid(errors1), Invalid(errors2)) => Invalid(errors1 append errors2)
        case (Invalid(errors), _) => Invalid(errors)
        case (_, Invalid(errors)) => Invalid(errors)
      }
      def unit[A](a: => A): Validation[E, A] = Valid(a)
    }
}

case class Valid[+E, +T](value: T) extends Validation[E, T] {
  def get = value
  def isValid = true

  def getOrElse[V >: T](default: => V) = value
  def orElse[F >: E, V >: T](default: => Validation[F, V]) = this

  def map[V](f: T => V) = Valid(f(value))
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]) =
    if (f(value)) this else Invalid(NonEmptyList(default.value))
}

case class Invalid[+E, +T](errors: NonEmptyList[E]) extends Validation[E, T] {
  def get = throw new NoSuchElementException
  def isValid = false

  def getOrElse[V](default: => V) = default
  def orElse[F >: E, V](default: => Validation[F, V]) = default

  def map[V](f: T => V) = this.asInstanceOf[Invalid[E, V]]
  def filter[F >: E](f: T => Boolean)(implicit default: Default[F]) = this
}

object Invalid {
  def apply[E](error: E): Invalid[E, Nothing] = Invalid(NonEmptyList(error))
}
