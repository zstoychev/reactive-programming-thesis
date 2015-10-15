package reactivethesis.worker

import akka.actor.Actor
import akka.pattern.pipe

import scala.concurrent.Future
import scala.util.control.NonFatal

trait Worker extends Actor {
  import Master._
  import context.dispatcher

  def doWork(processData: ProcessData): Future[Any]

  context.parent ! WorkerReady

  def receive: Receive = {
    case p@ProcessData(data, startTime) =>
      doWork(p)
        .map { DataProcessed(data, startTime, _) }
        .recover { case NonFatal(e) => ProcessFailed(data, startTime, e) }
        .pipeTo(context.parent)
  }
}
