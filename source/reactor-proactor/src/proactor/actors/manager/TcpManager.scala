package proactor.actors.manager

import akka.actor.{Actor, ActorRef, Props, Terminated}
import proactor.actors.manager.TcpConnectionHandlerProtocol._
import proactor.actors.manager.TcpManagerProtocol._
import proactor.reactor.{Proactor, ProactorProtocol, SocketChannelOperationsReceiver}

class TcpManager extends Actor {
  val proactor = new Proactor(ActorCompletionDispatcher)

  def receive: Receive = {
    case Bind(address) =>
      val bindActor = context.actorOf(Props(new TcpBindHandler(proactor, sender())))
      proactor.bind(address, bindActor)
    case Connect(address) =>
      val tcpConnectionActor = context.actorOf(Props(new TcpConnectionHandler(sender())))
      proactor.connect(address, tcpConnectionActor)
  }

  override def postStop(): Unit = proactor.stop()
}

class TcpBindHandler(proactor: Proactor[ActorRef], bindActor: ActorRef) extends Actor {
  override def preStart(): Unit = context.watch(bindActor)

  def receive: Receive = {
    case ProactorProtocol.Accepted(channel) =>
      val tcpConnectionActor = context.actorOf(Props(new TcpConnectionHandler(bindActor)))
      proactor.handle(channel, tcpConnectionActor)
    case ProactorProtocol.Closed => bindActor ! ProactorProtocol.Closed
    case Terminated(`bindActor`) => context.stop(self)
  }
}

class TcpConnectionHandler(receivingActor: ActorRef) extends Actor {
  def waitForHandlerInitialization: Receive = {
    case ProactorProtocol.Connected(operationsReceiver) =>
      receivingActor ! Connected(self)
      context.become(waitForChannelActorInitialization(operationsReceiver))
  }

  def waitForChannelActorInitialization(operationsReceiver: SocketChannelOperationsReceiver): Receive = {
    case Register(channelActor) =>
      operationsReceiver.startRead()
      context.watch(channelActor)
      context.become(process(operationsReceiver, channelActor))
  }

  def process(operationsReceiver: SocketChannelOperationsReceiver, channelActor: ActorRef): Receive = {
    case Write(data) => operationsReceiver.doWrite(data)
    case m: ProactorProtocol.Message => channelActor ! m
    case Terminated(`channelActor`) => context.stop(self)
  }

  def receive: Receive = waitForHandlerInitialization
}
