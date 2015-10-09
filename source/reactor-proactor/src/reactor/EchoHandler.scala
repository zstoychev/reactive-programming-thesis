package reactor

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, SocketChannel}

import reactor.basic.{CloseOnIOError, Handler, Reactor}

class EchoHandler(reactor: Reactor, channel: SocketChannel) extends Handler with CloseOnIOError {
  val key = reactor.registerHandler(channel, SelectionKey.OP_READ, this)
  private val buffer = ByteBuffer.allocate(1024)

  override def read(): Unit = safe {
    val size = channel.read(buffer)

    if (size > 0) key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE)
    else if (size < 0) channel.close()
  }

  override def write(): Unit = safe {
    buffer.flip()
    channel.write(buffer)
    if (!buffer.hasRemaining) key.interestOps(SelectionKey.OP_READ)
    buffer.compact()
  }
}
