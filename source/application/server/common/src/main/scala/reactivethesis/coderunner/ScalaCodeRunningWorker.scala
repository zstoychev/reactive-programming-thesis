package reactivethesis.coderunner

import javax.script.ScriptException

import reactivethesis.util.{Eval, FutureUtil}
import reactivethesis.worker.Master.{ProcessData, WorkData}
import reactivethesis.worker.Worker

import scala.concurrent.Future
import scala.concurrent.duration._

object ScalaCodeRunningWorker {
  case class StatusUpdate(status: String)
}

class ScalaCodeRunningWorker extends Worker {
  import ScalaCodeRunningWorker._
  val TimeLimit = 10.minutes
  val longRunningContext = context.system.dispatchers.lookup("long-running-dispatcher")

  def doWork(processData: ProcessData): Future[Any] = processData.data match {
    case WorkData(code: String, sender) =>
      object codeContext {
        def notify(message: Any) = sender ! StatusUpdate("" + message)
        def sleep(period: Long) = Thread.sleep(period)
      }
      implicit val ec = longRunningContext
      Future(Eval(code, ("context", codeContext)))
        .map { result => s"$result" }
        .recover {
          // ScriptException can contain unserializable data
          case e: ScriptException => throw new CodeExecutionException(e.toString)
        }
//    Този алтернативен код прекъсва изпълнението ако то не завърши до определено време.

//      val (result, cancel) = FutureUtil.runCancellably(Eval(code, ("context", codeContext))))
//
//
//      val canceller = context.system.scheduler.scheduleOnce(TimeLimit)(cancel())(context.dispatcher)
//      result foreach { _ => canceller.cancel() }
//
//      result.map(r => s"$r").recover { case e: ScriptException => throw new CodeExecutionException(e.toString) }
    case _ => Future.failed(new Exception("Invalid data format"))
  }
}

class CodeExecutionException(message: String) extends Exception(message)
