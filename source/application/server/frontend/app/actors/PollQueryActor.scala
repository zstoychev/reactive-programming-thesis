package actors

import akka.actor._
import akka.pattern.{AskTimeoutException, ask, pipe}
import akka.util.Timeout
import reactivethesis.poll.Poll
import reactivethesis.poll.Protocol.InvalidState
import reactivethesis.poll.actors.PollViewActor.QueryPoll

import scala.concurrent.duration._

class PollQueryActor(id: String, pollsViews: ActorRef, out: ActorRef) extends Actor {
  import context.dispatcher

  val registerTimeout = context.system.scheduler.scheduleOnce(4.seconds, self, PoisonPill)
  registerView()

  def registerView() = pollsViews ! (id, QueryPoll(streaming = true))

  def receive: Receive = unregistered

  def unregistered: Receive = {
    case poll: Poll =>
      out ! poll
      context.watch(sender())
      context.become(registered)
      registerTimeout.cancel()
    case InvalidState => context.stop(self)
  }

  def registered: Receive = {
    case poll: Poll => out ! poll
    case Terminated(view) =>
      context.become(unregistered)
      registerView()
  }
}
