package proactor.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import proactor.actors.manager.TcpConnectionHandlerProtocol._
import proactor.actors.manager.TcpManagerProtocol._
import proactor.actors.manager.TcpProactorExtension
import proactor.reactor.ProactorProtocol.{Closed, Received}

class EchoServerActor(port: Int) extends Actor {
  override def preStart(): Unit = TcpProactorExtension(context.system).tcpManager ! Bind(new InetSocketAddress(port))

  def receive: Receive = {
    case Connected(connectionActor) =>
      connectionActor ! Register(context.actorOf(Props(new EchoHandler(connectionActor))))
    case Closed => context.stop(self)
  }
}

class EchoHandler(connectionActor: ActorRef) extends Actor {
  def receive: Receive = {
    case Received(data) => sender() ! Write(data)
    case Closed => context.stop(self)
  }
}

object ActorEchoServer {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    system.actorOf(Props(new EchoServerActor(8000)))
  }
}
