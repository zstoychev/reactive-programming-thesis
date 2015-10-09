package proactor.reactor

import java.nio.channels.{SelectionKey, ServerSocketChannel}

import proactor.reactor.ProactorProtocol.{Accepted, Closed}
import reactor.basic.{CloseOnIOError, FullReactor, Handler}
import reactor.util._

class ServerSocketChannelHandler[CH](acceptorReactor: FullReactor, channel: ServerSocketChannel,
                                     completionDispatcher: CompletionDispatcher[CH],
                                     completionHandler: CH) extends Handler with CloseOnIOError {
  val key = acceptorReactor.registerHandler(channel, SelectionKey.OP_ACCEPT, this)

  override def cleanup(): Unit = {
    super.cleanup()
    completionDispatcher.dispatch(Closed, completionHandler)
  }

  override def accept() = safe {
    completionDispatcher.dispatch(Accepted(channel.accept().nonblocking.noTcpDelay), completionHandler)
  }
}
