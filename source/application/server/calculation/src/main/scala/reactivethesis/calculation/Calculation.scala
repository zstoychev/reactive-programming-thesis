package reactivethesis.calculation

import akka.actor.{Props, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import reactivethesis.coderunner.ScalaCodeRunningWorker
import reactivethesis.elastic.monitor.LittleLawMonitor
import reactivethesis.worker.Master

class Calculation(system: ActorSystem) {
  def start() = {
    val mediator = DistributedPubSub(system).mediator

    val codeProcessingMonitor = system.actorOf(
      LittleLawMonitor.props(parallelism => mediator ! Publish("parallelism", parallelism), 0.75, 0.5))
    val codeProcessingMaster = system.actorOf(
      Master.props(codeProcessingMonitor)(Props[ScalaCodeRunningWorker], Runtime.getRuntime.availableProcessors()),
      "codeProcessor"
    )
  }
}
