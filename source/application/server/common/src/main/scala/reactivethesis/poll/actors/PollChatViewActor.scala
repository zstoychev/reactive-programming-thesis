package reactivethesis.poll.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ActorRef, PoisonPill, ReceiveTimeout, Terminated}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.{PersistentView, SnapshotOffer}
import reactivethesis.poll.actors.PollChatActor.ChatMessagePosted
import reactivethesis.poll.actors.PollChatViewActor.{Messages, QueryMessages}
import reactivethesis.util.queue._

import scala.collection.immutable.Queue
import scala.concurrent.duration._

object PollChatViewActor {
  case class QueryMessages(streaming: Boolean = false)
  case class Messages(messages: Seq[(String, String)])

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case (id: String, command) => (id, command)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case (id: String, _) => (math.abs(id.hashCode) % 100).toString
  }
  val shardName: String = "PollChatView"
}

class PollChatViewActor(id: String, passivator: ActorRef, totalChatMessagesView: ActorRef) extends PersistentView {
  val MaxMessages = 100

  def persistenceId: String = s"poll-$id-chat"

  def viewId: String = s"poll-$id-chat-view"

  var messages = Queue.empty[(String, String)]

  var watchers = Set.empty[ActorRef]

  context.setReceiveTimeout(2.minutes)

  def receive: Receive = passivate orElse {
    case cm@ChatMessagePosted(name, message) if isPersistent =>
      messages = messages.enqueueBounded(MaxMessages)(name -> message)
      watchers foreach { _ ! (name, message) }

      totalChatMessagesView ! cm

      if (lastSequenceNr % MaxMessages == 0) saveSnapshot(messages)
    case SnapshotOffer(_, messagesSnapshot) => messages = messagesSnapshot.asInstanceOf[Queue[(String, String)]]
    case QueryMessages(streaming) =>
      sender() ! Messages(messages)
      if (streaming) {
        watchers += sender()
        context.watch(sender())
        context.setReceiveTimeout(Duration.Undefined)
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
