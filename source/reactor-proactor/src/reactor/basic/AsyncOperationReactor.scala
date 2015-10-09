package reactor.basic

import java.util.concurrent.ConcurrentLinkedQueue

import scala.util.control.NonFatal

trait AsyncOperationReactor extends Reactor {
  private val operationsQueue = new ConcurrentLinkedQueue[() => Unit]

  private def handleOperations() = {
    var operation = operationsQueue.poll()

    while (operation != null) {
      try { operation() } catch { case NonFatal(e) => e.printStackTrace() }
      operation = operationsQueue.poll()
    }
  }

  abstract override protected def preSelectJobs = (handleOperations _) :: super.preSelectJobs

  def receiveOperation(operation: => Unit): Unit = {
    operationsQueue.offer(() => operation)
    selector.wakeup()
  }
}
