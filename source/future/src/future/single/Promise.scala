package future.single

import functional.util._

class Promise[T] {
  sealed trait State
  case class Completed(value: Try[T]) extends State
  case class Pending(handlers: List[Try[T] => Unit]) extends State

  private var state: State = Pending(List.empty)

  val future: Future[T] = new Future[T] {
    def value: Option[Try[T]] = state match {
      case Completed(value) => Some(value)
      case Pending(_) => None
    }

    def onComplete(handler: Try[T] => Unit): Unit = state match {
      case Completed(value) => handler(value)
      case Pending(handlers) => state = Pending(handler :: handlers)
    }
  }

  def doComplete(value: Try[T]): Unit = state match {
    case Pending(handlers) =>
      state = Completed(value)
      handlers foreach { _(value) }
    case _ =>
  }

  def complete(value: Try[T]): Promise[T] = {
    doComplete(value)
    this
  }

  def succeed(value: T): Promise[T] = complete(Success(value))

  def fail(e: Throwable): Promise[T] = complete(Failure(e))
}

object Promise {
  def apply[T] = new Promise[T]
}
