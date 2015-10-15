package reactivethesis.sharding

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.ShardRegion.Passivate
import akka.pattern.BackoffSupervisor

import scala.concurrent.duration._

object ShardedEntityWithBackoff {
  case class PassivateRequest(sender: ActorRef, stopMessage: Any)
}

class ShardedEntityWithBackoff(props: (String, ActorRef) => Props) extends Actor {
  import ShardedEntityWithBackoff._
  val name = self.path.name

  val supervisor = context.actorOf(BackoffSupervisor.props(
    props(name, self),
    childName = name,
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2))

  def receive: Receive = {
    case Passivate(stopMessage) => context.parent ! Passivate(PassivateRequest(sender(), stopMessage))
    case PassivateRequest(sender, stopMessage) => sender ! stopMessage
    case message => supervisor forward message
  }
}
