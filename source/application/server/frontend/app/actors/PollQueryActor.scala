package actors

import akka.actor.{Actor, ActorRef, Status, Terminated}
import akka.pattern.{AskTimeoutException, ask, pipe}
import akka.util.Timeout
import reactivethesis.poll.Poll
import reactivethesis.poll.Protocol.InvalidState
import reactivethesis.poll.actors.PollViewActor.QueryPoll

import scala.concurrent.duration._

class PollQueryActor(id: String, pollsViews: ActorRef, out: ActorRef) extends Actor {
  import context.dispatcher

  implicit val timeout = Timeout(4.seconds)

  registerView()

  def registerView() = (pollsViews ? (id, QueryPoll(streaming = true))) pipeTo self

  def receive: Receive = unregistered

  def unregistered: Receive = {
    case poll: Poll =>
      out ! poll
      context.watch(sender())
      context.become(registered)
    case InvalidState => context.stop(self)
    case Status.Failure(e: AskTimeoutException) => registerView()
  }

  def registered: Receive = {
    case poll: Poll => out ! poll
    case Terminated(view) =>
      context.become(unregistered)
      registerView()
  }
}
