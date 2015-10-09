package reactor.basic.acceptor

import java.nio.channels.{SelectionKey, ServerSocketChannel, SocketChannel}

import reactor.basic.{CloseOnIOError, Handler, Reactor}
import reactor.util._

class CommonAcceptor[R <: Reactor](reactor: R, channel: ServerSocketChannel)
                                  (setupHandler: (R, SocketChannel) => Unit) extends Handler with CloseOnIOError {
  val key = reactor.registerHandler(channel, SelectionKey.OP_ACCEPT, this)

  override def accept(): Unit = safe {
    setupHandler(reactor, channel.accept().nonblocking.noTcpDelay)
  }
}
