package proactor.reactor

import java.nio.channels.{SelectionKey, SocketChannel}

import pool._
import proactor.reactor.ProactorProtocol._
import reactor.basic.{CloseOnIOError, FullReactor, Handler}
import reactor.util._

class SocketChannelHandler[CH](reactor: FullReactor, channel: SocketChannel,
                               completionDispatcher: CompletionDispatcher[CH], completionHandler: CH)
    extends Handler with CloseOnIOError with SocketChannelOperationsReceiver {
  val key = reactor.registerHandler(
    channel, if (channel.isConnected) 0 else SelectionKey.OP_CONNECT, this)
  private var pendingWrites = Vector.empty[ArrayBuffer]

  if (channel.isConnected) dispatch(Connected(this))

  private def dispatch(message: Message) = completionDispatcher.dispatch(message, completionHandler)

  def startRead(): Unit = reactor.receiveOperation(key.interestOps(key.interestOps() | SelectionKey.OP_READ))
  def stopRead(): Unit = reactor.receiveOperation(key.interestOps(key.interestOps() & ~SelectionKey.OP_READ))

  def doWrite(data: Array[Byte]): Unit = reactor.receiveOperation {
    pendingWrites = pendingWrites :+ (data, 0)
    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE)
  }

  override def cleanup(): Unit = {
    super.cleanup()
    dispatch(Closed)
  }

  override def connect(): Unit = safe {
    if (channel.finishConnect()) {
      key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT)
      dispatch(Connected(this))
    }
    else cleanup()
  }

  override def read(): Unit = safe {
    reactor.byteBufferPool.withBuffer { inputBuffer =>
      val size = channel.read(inputBuffer)

      if (size == -1) { cleanup(); return }

      dispatch(Received(inputBuffer.toByteArray))
    }
  }

  override def write(): Unit = safe {
    val output = pendingWrites.head
    reactor.byteBufferPool.withBuffer(Some(output)) { channel.write(_) } match {
      case Some(left) => pendingWrites = left +: pendingWrites.tail
      case None => pendingWrites = pendingWrites.tail
    }
    if (pendingWrites.isEmpty) {
      dispatch(WriteFinished)
      key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE)
    }
  }
}
