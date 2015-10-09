package reactor.basic

import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.util.control.NonFatal

case class TimeoutHandler(handler: () => Unit, atTime: Long)

trait TimeoutReactor extends Reactor {
  private var currentTime = System.currentTimeMillis
  private val timeouts = new mutable.PriorityQueue[TimeoutHandler]()(Ordering.by(-_.atTime))

  private def handleTimeouts() = {
    currentTime = System.currentTimeMillis

    while (!timeouts.isEmpty && timeouts.head.atTime <= currentTime) {
      try { timeouts.dequeue().handler() } catch { case NonFatal(e) => }
    }
  }

  override protected def nextTimeout = timeouts.headOption map { _.atTime - currentTime } getOrElse 0L

  abstract override protected def preSelectJobs = (handleTimeouts _) :: super.preSelectJobs

  def afterTimeout(timeout: Duration = 0.millis)(handler: => Unit): Unit = {
    timeouts += TimeoutHandler(() => handler, currentTime + timeout.toMillis)
  }
}
