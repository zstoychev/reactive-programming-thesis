package reactivethesis.worker

import akka.actor._
import reactivethesis.elastic.monitor.ProcessTimeTicker

import scala.collection.immutable.Queue
import scala.util.{Failure, Success}

object Master {
  case class Work(work: Any)
  case class WorkData(work: Any, sender: ActorRef)
  case class ProcessData(data: WorkData, startTime: Long)
  case class DataProcessed(data: WorkData, startTime: Long, result: Any)
  case class ProcessFailed(data: WorkData, startTime: Long, reason: Throwable)
  case object WorkerReady

  val MaxQueueSize = 20

  def props(statsMonitor: ActorRef)(worker: Props, numberOfWorkers: Int) =
    Props(new Master(statsMonitor)(worker, numberOfWorkers))
}

class Master(val statsMonitor: ActorRef)(worker: Props, val numberOfWorkers: Int) extends Actor with ProcessTimeTicker {
  import Master._

  var pendingWorkers = Queue.empty[ActorRef]
  var pendingWork = Queue.empty[WorkData]

  val workers = List.fill(numberOfWorkers)(context.actorOf(worker))

  def processWork(): Unit = {
    val size = math.min(pendingWorkers.size, pendingWork.size)
    pendingWorkers zip pendingWork foreach { case (worker, work) =>
      worker ! ProcessData(work, System.currentTimeMillis)
    }
    pendingWorkers = pendingWorkers.drop(size)
    pendingWork = pendingWork.drop(size)
  }

  def addToPending(worker: ActorRef) = {
    pendingWorkers = pendingWorkers enqueue sender()
    processWork()
  }

  def masterBehaviour: Receive = {
    case Work(work) =>
      logNewRequest()
      if (pendingWork.size >= MaxQueueSize) sender() ! Failure(new Exception)
      else {
        pendingWork = pendingWork enqueue WorkData(work, sender())
        processWork()
      }
    case DataProcessed(data, startTime, result) =>
      logProcessTime(startTime)
      addToPending(sender())
      data.sender ! Success(result)
    case ProcessFailed(data, startTime, reason) =>
      logProcessTime(startTime)
      addToPending(sender())
      data.sender ! Failure(reason)
    case WorkerReady => addToPending(sender())
  }

  def receive: Receive = masterBehaviour orElse processTimeTick
}
