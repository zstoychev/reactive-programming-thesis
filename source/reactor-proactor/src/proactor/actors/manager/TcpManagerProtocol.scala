package proactor.actors.manager

import java.net.InetSocketAddress

import akka.actor.ActorRef

object TcpManagerProtocol {
  case class Bind(address: InetSocketAddress)
  case class Connect(address: InetSocketAddress)
}

object TcpConnectionHandlerProtocol {
  case class Connected(connectionActor: ActorRef)
  case class Register(handlerActor: ActorRef)
  case class Write(data: Array[Byte])
}
