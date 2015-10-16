package reactivethesis.sharding

import akka.actor._
import akka.cluster.sharding.ShardRegion.Passivate
import akka.pattern.BackoffSupervisor

import scala.concurrent.duration._

object ShardedEntityWithBackoff {
  case object RequestPassivate
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

  context.watch(supervisor)

  def receive: Receive = {
    case RequestPassivate => context.parent ! Passivate(PoisonPill)
    case PoisonPill => context.stop(self)
    case Terminated(`supervisor`) => context.stop(self)
    case message => supervisor forward message
  }
}
