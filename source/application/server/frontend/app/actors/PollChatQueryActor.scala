package actors

import akka.actor.{Actor, ActorRef, Status, Terminated}
import akka.pattern.{AskTimeoutException, ask, pipe}
import akka.util.Timeout
import reactivethesis.poll.Protocol.InvalidState
import reactivethesis.poll.actors.PollChatViewActor.{Messages, QueryMessages}

import scala.concurrent.duration._

class PollChatQueryActor(id: String, pollsChatViews: ActorRef, out: ActorRef) extends Actor {
  import context.dispatcher

  implicit val timeout = Timeout(4.seconds)

  registerView()

  def registerView() = (pollsChatViews ? (id, QueryMessages(streaming = true))) pipeTo self

  def receive: Receive = unregistered

  def unregistered: Receive = {
    case Messages(messages) =>
      messages foreach { out ! _ }
      context.watch(sender())
      context.become(registered)
    case message@(_: String, _: String) =>
      out ! message
      context.watch(sender())
      context.become(registered)
    case InvalidState => context.stop(self)
    case Status.Failure(e: AskTimeoutException) => registerView()
  }

  def registered: Receive = {
    case Messages(messages) => messages foreach { out ! _ }
    case message@(_: String, _: String) => out ! message
    case Terminated(view) =>
      context.become(unregistered)
      registerView()
  }
}
