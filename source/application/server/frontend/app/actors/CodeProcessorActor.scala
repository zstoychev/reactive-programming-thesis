package actors

import akka.actor.{ActorRef, Actor}
import reactivethesis.coderunner.ScalaCodeRunningWorker.StatusUpdate
import reactivethesis.worker.Master.Work

import scala.util.{Failure, Success}

class CodeProcessorActor(codeProcessingRouter: ActorRef, out: ActorRef) extends Actor {
  def receive: Receive = {
    case code: String =>
      codeProcessingRouter ! Work(code)
      context.become(processing)
  }

  def processing: Receive = {
    case StatusUpdate(status) => out ! status
    case Success(result) =>
      out ! ("" + result) // using quotes for avoiding null
      context.stop(self)
    case Failure(e) =>
      out ! e.toString
      context.stop(self)
  }
}
