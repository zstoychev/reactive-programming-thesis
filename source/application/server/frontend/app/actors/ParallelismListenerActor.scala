package actors

import akka.actor.{ActorRef, Actor}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe

class ParallelismListenerActor(out: ActorRef) extends Actor {
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("parallelism", self)

  def receive: Receive = {
    case parallelism: Int => out ! parallelism.toString
  }
}
