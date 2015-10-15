package reactivethesis.poll.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ActorRef, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.{Update, PersistentActor, RecoveryCompleted, SnapshotOffer}
import reactivethesis.poll.Protocol.PollCommand
import reactivethesis.poll.{PollState, Protocol}

import scala.concurrent.duration._

object PollActor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case (id: String, c: PollCommand) => (id, c)
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case (id: String, _) => (math.abs(id.hashCode) % 100).toString
  }
  val shardName: String = "Poll"
}

class PollActor(id: String, passivator: ActorRef, pollViews: ActorRef) extends PersistentActor {
  import PollState._
  import Protocol._
  val SnapshotTarget = 20

  def persistenceId: String = s"poll-$id"

  var state: Option[PollState] = None

  context.setReceiveTimeout(2.minutes)

  def behaviour(state: Receive) = state orElse passivate orElse invalidCommand
  def complete(event: PollEvent, reply: Any, newState: => Receive) = persist(event) { event =>
    state = updateState(state, event)
    if (lastSequenceNr % SnapshotTarget == 0) saveSnapshot(state)
    context.become(newState)
    pollViews ! (id, Update(await = true))
  }

  def receiveCommand: Receive = behaviour(notStarted)

  def notStarted: Receive = {
    case StartPoll(options) => complete(PollStarted(id, options), StartPollAck(id), behaviour(started(state.get)))
  }

  def started(ps: PollState): Receive = {
    case AnswerPoll(name, optionsAnswers) if ps.poll.options.size != optionsAnswers.size =>
      sender() ! InvalidState
    case AnswerPoll(name, optionsAnswers) =>
      val answerId = ps.totalAnswers + 1
      complete(PollAnswered(answerId, name, optionsAnswers), AnswerPollAck(answerId), behaviour(started(state.get)))

    case UpdatePollAnswer(id, optionsAnswers)
      if ps.poll.options.size != optionsAnswers.size || !ps.poll.answers.exists(_.id == id) =>
      sender() ! InvalidState
    case UpdatePollAnswer(id, optionsAnswers) =>
      complete(PollAnswerUpdated(id, optionsAnswers), UpdatePollAnswerAck, behaviour(started(state.get)))

    case RemovePollAnswer(id) if !ps.poll.answers.exists(_.id == id) =>
      sender() ! InvalidState
    case RemovePollAnswer(id) =>
      complete(PollAnswerRemoved(id), RemovePollAnswerAck, behaviour(started(state.get)))
  }

  def passivate: Receive = {
    case ReceiveTimeout => passivator ! Passivate(stopMessage = Stop)
    case Stop =>
      context.stop(self)
      context.parent ! PoisonPill
  }

  def invalidCommand: Receive = {
    case _: PollChatCommand => sender() ! InvalidState
  }

  def receiveRecover: Receive = {
    case event: PollEvent => state = updateState(state, event)
    case SnapshotOffer(_, snapshot) => state = snapshot.asInstanceOf[Option[PollState]]
    case RecoveryCompleted => context.become(state match {
      case Some(ps) => behaviour(started(ps))
      case None => behaviour(notStarted)
    })
  }
}
