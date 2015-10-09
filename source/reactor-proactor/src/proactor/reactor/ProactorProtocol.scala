package proactor.reactor

import java.nio.channels.SocketChannel

object ProactorProtocol {
  trait Message
  case class Accepted(channel: SocketChannel) extends Message
  case class Connected(operationsReceiver: SocketChannelOperationsReceiver) extends Message
  case class Received(data: Array[Byte]) extends Message
  case object WriteFinished extends Message
  case object Closed extends Message
}
