package future.concurrent

import java.util.concurrent.Executor

import functional.util._
import functional.{Monad, MonadWithZero}
import future.concurrent.Future.tryF

import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import scala.util.control.NonFatal

trait Future[+T] extends Awaitable[T] {
  def value: Option[Try[T]]
  def onComplete(handler: Try[T] => Unit)(implicit ex: Executor): Unit

  def isComplete: Boolean = !value.isEmpty

  def map[V](f: T => V)(implicit ex: Executor): Future[V] = {
    val p = Promise[V]
    onComplete { value => p complete (value map f) }
    p.future
  }

  def flatMap[V](f: T => Future[V])(implicit ex: Executor): Future[V] = {
    val p = Promise[V]
    onComplete {
      case Success(value) => tryF(f(value)) onComplete { p complete _ }
      case Failure(e) => p.fail(e)
    }
    p.future
  }

  def filter(f: T => Boolean)(implicit ex: Executor): Future[T] = {
    val p = Promise[T]
    onComplete { value => p complete (value filter f) }
    p.future
  }

  def withFilter(f: T => Boolean)(implicit ex: Executor): Future[T] = filter(f)

  def recover[V >: T](f: PartialFunction[Throwable, V])(implicit ex: Executor): Future[V] = {
    val p = Promise[V]
    onComplete { value => p complete (value recover f) }
    p.future
  }

  def recoverWith[V >: T](f: PartialFunction[Throwable, Future[V]])(implicit ex: Executor): Future[V] = {
    val p = Promise[V]
    onComplete {
      case Success(value) => p succeed  value
      case Failure(e) =>
        if (f.isDefinedAt(e)) tryF(f(e)) onComplete { p complete _ }
        else p.fail(e)
    }
    p.future
  }

  def foreach(f: T => Unit)(implicit ex: Executor): Unit = onComplete { _ foreach f }
}

object Future {
  implicit def futureMonad(implicit ex: Executor) = new MonadWithZero[Future] {
    def mzero[A]: Future[A] = Future.failed(new NoSuchElementException)
    def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] = m.flatMap(f)
    def unit[A](a: => A): Future[A] = Future(a)
  }

  def apply[T](value: => T)(implicit ex: Executor) = {
    val p = Promise[T]
    ex.execute(new Runnable {
      def run(): Unit = p.succeed(value)
    })
    p.future
  }
  def successful[T](value: T) = resolved(Success(value))
  def failed[T](e: Throwable) = resolved(Failure(e))

  def tryF[T](f: => Future[T]) = try f catch { case NonFatal(e) => Future.failed(e) }

  def resolved[T](r: Try[T]) = new Future[T] {
    val value: Option[Try[T]] = Some(r)
    def onComplete(handler: (Try[T]) => Unit)(implicit ex: Executor): Unit =
      ex.execute(new Runnable() { def run = handler(r) })

    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = this
    def result(atMost: Duration)(implicit permit: CanAwait): T = r.get
  }

  def firstOf[T](futures: Seq[Future[T]])(implicit ex: Executor) = {
    val p = Promise[T]
    futures foreach { _ onComplete p.complete }
    p.future
  }

  def firstSuccessfulOf[T](futures: Seq[Future[T]])(implicit ex: Executor) = {
    val p = Promise[T]
    futures foreach { _ foreach { p.succeed(_) } }
    Monad.sequence(futures.toList) onComplete { case Failure(_) => p.fail(new NoSuchElementException) case _ => }
    p.future
  }
}
