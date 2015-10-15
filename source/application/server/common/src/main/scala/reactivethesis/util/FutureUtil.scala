package reactivethesis.util

import java.util.concurrent.CancellationException

import scala.concurrent.{ExecutionContext, Future}

object FutureUtil {
  def runCancellably[T](expr: => T)(implicit ec: ExecutionContext): (Future[T], () => Unit) = {
    var currentThread: Option[Thread] = None
    var isCancelled = false
    val lock = new Object

    val future = Future {
      lock.synchronized {
        if (isCancelled) throw new InterruptedException()
        else currentThread = Some(Thread.currentThread)
      }
      val result = expr
      lock.synchronized {
        currentThread = None
      }
      result
    } recoverWith {
      case _: InterruptedException => Future.failed(new CancellationException)
    }

    def cancel(): Unit = lock.synchronized {
      isCancelled = true
      currentThread foreach { _.interrupt() }
    }

    (future, cancel)
  }
}
