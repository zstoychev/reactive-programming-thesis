package actors

import akka.actor._
import reactivethesis.poll.Protocol.InvalidState
import reactivethesis.poll.actors.PollChatViewActor.{Messages, QueryMessages}

import scala.concurrent.duration._

class PollChatQueryActor(id: String, pollsChatViews: ActorRef, out: ActorRef) extends Actor {
  import context.dispatcher

  val registerTimeout = context.system.scheduler.scheduleOnce(4.seconds, self, PoisonPill)
  registerView()

  def registerView() = pollsChatViews ! (id, QueryMessages(streaming = true))

  def receive: Receive = unregistered

  def unregistered: Receive = {
    case Messages(messages) =>
      messages foreach { out ! _ }
      context.watch(sender())
      context.become(registered)
      registerTimeout.cancel()
    case message@(_: String, _: String) =>
      out ! message
      context.watch(sender())
      context.become(registered)
    case InvalidState => context.stop(self)
  }

  def registered: Receive = {
    case Messages(messages) => messages foreach { out ! _ }
    case message@(_: String, _: String) => out ! message
    case Terminated(view) =>
      context.become(unregistered)
      registerView()
  }
}
