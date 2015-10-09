package reactor.basic.acceptor

import java.nio.channels.{SelectionKey, ServerSocketChannel, SocketChannel}

import reactor.basic.{CloseOnIOError, AsyncOperationReactor, Handler, Reactor}
import reactor.util._

class WorkersAcceptor[A <: AsyncOperationReactor, R <: AsyncOperationReactor]
    (acceptorReactor: A, workerReactors: List[R], channel: ServerSocketChannel)
    (setupHandler: (R, SocketChannel) => Unit) extends Handler with CloseOnIOError {
  val key = acceptorReactor.registerHandler(channel, SelectionKey.OP_ACCEPT, this)
  private var currentReactor = 0

  private def nextReactor: R = {
    val result = workerReactors(currentReactor)
    currentReactor = (currentReactor + 1) % workerReactors.size

    result
  }

  override def accept(): Unit = safe {
    val reactor = nextReactor
    val socketChannel = channel.accept().nonblocking.noTcpDelay
    reactor.receiveOperation(setupHandler(reactor, socketChannel))
  }
}
