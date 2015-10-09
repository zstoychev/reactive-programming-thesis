package reactor

import java.nio.channels.{SelectionKey, SocketChannel}

import play.api.libs.iteratee._
import pool._
import reactor.basic.{CloseOnIOError, FullReactor, Handler}
import reactor.util._

import scala.concurrent.ExecutionContext.Implicits.global

class IterateeHandler(reactor: FullReactor, channel: SocketChannel)
                     (in: Iteratee[Array[Byte], _], out: Enumerator[Array[Byte]]) extends Handler with CloseOnIOError {
  val key: SelectionKey = reactor.registerHandler(channel, SelectionKey.OP_READ, this)

  private var currentIn = in
  private var pendingWrites = Vector.empty[ArrayBuffer]
  private val outputIteratee: Iteratee[Array[Byte], Unit] = Cont {
    case Input.El(bytes) =>
      reactor.receiveOperation({
        pendingWrites = pendingWrites :+ (bytes, 0)
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE)
      })
      outputIteratee
    case Input.Empty => outputIteratee
    case Input.EOF =>
      reactor.receiveOperation(cleanup())
      Done(())
  }

  (out |>> outputIteratee) map { _.run }

  override def read(): Unit = safe {
    reactor.byteBufferPool.withBuffer { inputBuffer =>
      val size = channel.read(inputBuffer)

      key.interestOps(key.interestOps() & ~SelectionKey.OP_READ)

      if (size == -1) Enumerator.eof |>> currentIn
      else (Enumerator(inputBuffer.toByteArray) |>> currentIn) foreach { it =>
        reactor.receiveOperation({
          currentIn = in
          key.interestOps(key.interestOps() | SelectionKey.OP_READ)
        })
      }
    }
  }

  override def write(): Unit = safe {
    val output = pendingWrites.head
    reactor.byteBufferPool.withBuffer(Some(output)) { channel.write(_) } match {
      case Some(left) => pendingWrites = left +: pendingWrites.tail
      case None => pendingWrites = pendingWrites.tail
    }
    if (pendingWrites.isEmpty) key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE)
  }
}
