package reactivethesis.poll.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{PoisonPill, ReceiveTimeout, Terminated, ActorRef}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.{SnapshotOffer, PersistentView}
import reactivethesis.poll.PollState
import reactivethesis.poll.PollState._
import reactivethesis.poll.Protocol.{InvalidState, PostChatMessage}
import reactivethesis.poll.actors.PollChatViewActor.QueryMessages

import scala.collection.immutable.Queue
import scala.concurrent.duration._

object PollViewActor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case (id: String, command) => (id, command)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case (id: String, _) => (math.abs(id.hashCode) % 100).toString
  }
  val shardName: String = "PollView"

  case class QueryPoll(streaming: Boolean = false)
}

class PollViewActor(id: String, passivator: ActorRef) extends PersistentView {
  import PollViewActor._

  def persistenceId: String = s"poll-$id"

  def viewId: String = s"poll-$id-view"

  var state = Option.empty[PollState]

  var watchers = Set.empty[ActorRef]

  context.setReceiveTimeout(2.minutes)

  def receive: Receive = passivate orElse {
    case event: PollEvent => state = updateState(state, event)
    case SnapshotOffer(_, stateSnapshot) => state = stateSnapshot.asInstanceOf[Option[PollState]]
    case QueryPoll(streaming) => state match {
      case Some(pollState) =>
        sender() ! pollState.poll
        if (streaming) {
          watchers += sender()
          context.watch(sender())
          context.setReceiveTimeout(Duration.Undefined)
        }
      case None => sender() ! InvalidState
    }
    case Terminated(watcher) =>
      watchers -= watcher
      if (watchers.isEmpty) context.setReceiveTimeout(2.minutes)
  }

  def passivate: Receive = {
    case ReceiveTimeout => passivator ! Passivate(stopMessage = Stop)
    case Stop =>
      context.stop(self)
      context.parent ! PoisonPill
  }
}
