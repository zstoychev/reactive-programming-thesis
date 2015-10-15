package reactivethesis.elastic.monitor

import akka.actor.{Props, Actor, ActorRef, Terminated}
import reactivethesis.util.queue._

import scala.collection.immutable.Queue
import scala.concurrent.duration.Duration

object LittleLawMonitor {
  val NumberOfStats = 60
  case class Stat(numberOfWorkers: Int, requestsPerSecond: Int, requestResponseTime: Duration)

  def props(setParallelNeed: Int => Unit, increaseBound: Double, decreaseBound: Double) =
    Props(new LittleLawMonitor(setParallelNeed, increaseBound, decreaseBound))
}

class LittleLawMonitor(setParallelNeed: Int => Unit, increaseBound: Double, decreaseBound: Double) extends Actor {
  import LittleLawMonitor._

  val initialStats = Map.empty[ActorRef, Queue[Stat]].withDefaultValue(Queue.empty)
  var stats = initialStats

  def updateLoad(): Unit = if (hasEnoughData) {
    val avgParallelNeed = stats.mapValues(parallelNeed).values.sum / stats.size
    val proportion = avgParallelNeed / stats.size

    if (proportion > increaseBound || proportion < decreaseBound) {
      setParallelNeed(math.ceil(avgParallelNeed).toInt)
      stats = initialStats
    }
  }

  def parallelNeed(stats: Queue[Stat]) = stats.map({
    case Stat(numberOfWorkers, requestPerSecond, requestResponseTime) =>
      (requestPerSecond * requestResponseTime.toMillis) / (numberOfWorkers * 1000)
  }).sum / stats.size

  def hasEnoughData = stats.forall { case (_, queue) => queue.size >= NumberOfStats }

  def receive: Receive = {
    case stat: Stat =>
      if (!stats.contains(sender())) context.watch(sender())
      stats += sender() -> stats(sender()).enqueueBounded(NumberOfStats)(stat)
      updateLoad()

    case Terminated(actor) => stats -= actor
  }
}
