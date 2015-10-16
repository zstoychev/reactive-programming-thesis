package reactivethesis.poll.actors

import akka.actor.{ActorRef, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer, Update}
import reactivethesis.poll.Protocol
import reactivethesis.poll.actors.PollChatActor.{ChatMessagePosted, PollChatInitialized}
import reactivethesis.sharding.ShardedEntityWithBackoff.RequestPassivate

import scala.concurrent.duration._

object PollChatActor {
  case class PollChatInitialized(id: String)
  case class ChatMessagePosted(name: String, message: String)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case (id: String, command) => (id, command)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case (id: String, _) => (math.abs(id.hashCode) % 100).toString
  }
  val shardName: String = "PollChat"
}

class PollChatActor(id: String, passivator: ActorRef, chatViews: ActorRef) extends PersistentActor {
  import Protocol._
  val SnapshotTarget = 100

  def persistenceId: String = s"poll-$id-chat"

  context.setReceiveTimeout(2.minutes)

  def behaviour(state: Receive) = state orElse passivate orElse invalidCommand

  def receiveCommand: Receive = behaviour(uninitialized)

  def uninitialized: Receive = {
    case InitializePollChat => persist(PollChatInitialized) { _ =>
      sender() ! InitializePollChatAck
      context.become(behaviour(initialized))
    }
  }

  def initialized: Receive = {
    case PostChatMessage(name, message) => persist(ChatMessagePosted(name, message)) { _ =>
      sender() ! PostChatMessageAck
      if (lastSequenceNr % SnapshotTarget == 0) saveSnapshot(true)
      chatViews ! (id, Update(await = true))
    }
  }

  def passivate: Receive = {
    case ReceiveTimeout => passivator ! RequestPassivate
  }

  def invalidCommand: Receive = {
    case _: PollChatCommand => sender() ! InvalidState
  }

  var isInitializedAfterRecovery = false

  def receiveRecover: Receive = {
    case PollChatInitialized => isInitializedAfterRecovery = true
    case ChatMessagePosted =>
    case SnapshotOffer(_, isInitialized) => isInitializedAfterRecovery = isInitialized.asInstanceOf[Boolean]
    case RecoveryCompleted =>
      context.become(if (isInitializedAfterRecovery) behaviour(initialized) else behaviour(uninitialized))
  }
}
