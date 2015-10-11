package future.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

import functional.util._

import scala.annotation.tailrec
import scala.concurrent.{TimeoutException, CanAwait}
import scala.concurrent.duration.Duration

class Promise[T] {
  case class Handler(handler: Try[T] => Any, ex: Executor) {
    def executeWithValue(value: Try[T]) = ex.execute(new Runnable {
      def run(): Unit = handler(value)
    })
  }

  sealed trait State
  case class Completed(value: Try[T]) extends State
  case class Pending(handlers: List[Handler]) extends State

  private val state = new AtomicReference[State](Pending(List.empty))

  @tailrec
  private def executeWhenComplete(handler: Handler): Unit = state.get() match {
    case Completed(value) => handler.executeWithValue(value)
    case s@Pending(handlers) => if (!state.compareAndSet(s, Pending(handler :: handlers))) executeWhenComplete(handler)
  }

  @tailrec
  private def completeWithValue(value: Try[T]): List[Handler] = state.get() match {
    case Completed(_) => List.empty
    case s@Pending(handlers) => if (state.compareAndSet(s, Completed(value))) handlers else completeWithValue(value)
  }

  val future: Future[T] = new Future[T] {
    def value: Option[Try[T]] = state.get() match {
      case Completed(value) => Some(value)
      case Pending(_) => None
    }

    def onComplete(handler: Try[T] => Unit)(implicit ex: Executor): Unit = executeWhenComplete(Handler(handler, ex))

    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
      if (atMost > Duration.Zero && !isComplete) {
        val thread = Thread.currentThread
        onComplete(_ => LockSupport.unpark(thread))(Executors.currentThreadExecutor)

        if (atMost == Duration.Inf) LockSupport.park()
        else LockSupport.parkNanos(atMost.toNanos)
      }

      if (isComplete) this
      else throw new TimeoutException
    }

    def result(atMost: Duration)(implicit permit: CanAwait): T =
      ready(atMost).value.get.get
  }

  def complete(value: Try[T]): Promise[T] = {
    completeWithValue(value) foreach { _.executeWithValue(value) }
    this
  }

  def succeed(value: T): Promise[T] = complete(Success(value))

  def fail(e: Throwable): Promise[T] = complete(Failure(e))
}

object Promise {
  def apply[T] = new Promise[T]
}
