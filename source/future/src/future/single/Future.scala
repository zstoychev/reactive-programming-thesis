package future.single

import functional.MonadWithZero
import functional.util._
import future.single.Future.tryF

import scala.util.control.NonFatal

trait Future[+T] {
  def value: Option[Try[T]]
  def onComplete(handler: Try[T] => Unit): Unit

  def isComplete: Boolean = !value.isEmpty

  def map[V](f: T => V): Future[V] = {
    val p = Promise[V]
    onComplete { value => p complete (value map f) }
    p.future
  }

  def flatMap[V](f: T => Future[V]): Future[V] = {
    val p = Promise[V]
    onComplete {
      case Success(value) => tryF(f(value)) onComplete p.complete
      case Failure(e) => p.fail(e)
    }
    p.future
  }

  def filter(f: T => Boolean): Future[T] = {
    val p = Promise[T]
    onComplete { value => p complete (value filter f) }
    p.future
  }

  def withFilter(f: T => Boolean): Future[T] = filter(f)

  def recover[V >: T](f: PartialFunction[Throwable, V]): Future[V] = {
    val p = Promise[V]
    onComplete { value => p complete (value recover f) }
    p.future
  }

  def recoverWith[V >: T](f: PartialFunction[Throwable, Future[V]]): Future[V] = {
    val p = Promise[V]
    onComplete {
      case Success(value) => p succeed  value
      case Failure(e) =>
        if (f.isDefinedAt(e)) tryF(f(e)) onComplete { p complete _ }
        else p.fail(e)
    }
    p.future
  }

  def foreach(f: T => Unit): Unit = onComplete { _ foreach f }
}

object Future {
  implicit val futureMonad = new MonadWithZero[Future] {
    def mzero[A]: Future[A] = Future.failed(new NoSuchElementException)
    def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] = m.flatMap(f)
    def unit[A](a: => A): Future[A] = Future(a)
  }

  def apply[T](value: => T) = Promise[T].succeed(value).future
  def successful[T](value: T) = Future(value)
  def failed[T](e: Throwable) = Promise[T].fail(e).future

  def tryF[T](f: => Future[T]) = try f catch { case NonFatal(e) => Future.failed(e) }

  def firstOf[T](futures: Seq[Future[T]]) = {
    val p = Promise[T]
    futures foreach { _ onComplete p.complete }
    p.future
  }
}
