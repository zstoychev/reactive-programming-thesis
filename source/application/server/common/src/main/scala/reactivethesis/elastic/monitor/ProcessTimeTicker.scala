package reactivethesis.elastic.monitor

import akka.actor.{Actor, ActorRef}
import reactivethesis.elastic.monitor.LittleLawMonitor.Stat

import scala.concurrent.duration._

object ProcessTimeTicker {
  case object StatTick
}

trait ProcessTimeTicker extends Actor {
  import ProcessTimeTicker._
  import context.dispatcher

  def statsMonitor: ActorRef
  def numberOfWorkers: Int

  // since last tick
  var requests = 0

  // for all time. Can be changed to something smarter like e.g. last 10 minutes
  var processTimeSum = 0L
  var processedData = 0

  val tick = context.system.scheduler.schedule(1.second, 1.second, self, StatTick)

  def processTimeTick: Receive = {
    case StatTick =>
      val avgProcessTime = if (processedData == 0) 0 else processTimeSum / processedData
      statsMonitor ! Stat(numberOfWorkers, requests, avgProcessTime.millis)
      requests = 0
  }

  def logProcessTime(startTime: Long) = {
    processedData += 1
    processTimeSum += System.currentTimeMillis - startTime
  }

  def logNewRequest() = {
    requests += 1
  }

  abstract override def postStop(): Unit = {
    super.postStop()
    tick.cancel()
  }
}
