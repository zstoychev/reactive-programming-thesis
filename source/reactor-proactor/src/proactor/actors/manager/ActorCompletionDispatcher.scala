package proactor.actors.manager

import akka.actor.ActorRef
import proactor.reactor.CompletionDispatcher
import proactor.reactor.ProactorProtocol.Message

object ActorCompletionDispatcher extends CompletionDispatcher[ActorRef] {
  def dispatch(message: Message, receiver: ActorRef): Unit = receiver ! message
}
