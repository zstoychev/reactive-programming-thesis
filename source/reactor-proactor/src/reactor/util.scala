package reactor

import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectableChannel}

object util {
  implicit class RichSelectableChannel[T <: SelectableChannel](val channel: T) extends AnyVal {
    def nonblocking = {
      channel.configureBlocking(false)
      channel
    }
  }

  implicit class RichSocketChannel[T <: SocketChannel](val channel: T) extends AnyVal {
    def noTcpDelay = {
      channel.setOption[java.lang.Boolean](StandardSocketOptions.TCP_NODELAY, true)
      channel
    }
  }

  implicit class RichByteBuffer(val buffer: ByteBuffer) extends AnyVal {
    def fromByteArray(array: Array[Byte])(size: Int = array.length)(handler: ByteBuffer => Unit): Unit = {
      buffer.put(array, 0, size)
      buffer.flip()
      handler(buffer)
      buffer.clear()
    }

    def toByteArray: Array[Byte] = {
      buffer.flip()
      val result = new Array[Byte](buffer.remaining())
      buffer.get(result)
      result
    }
  }

  implicit class RichByteArray(val array: Array[Byte]) extends AnyVal {
    def toByteBuffer: ByteBuffer = {
      val result = ByteBuffer.allocate(array.length)
      result.put(array)
      result.flip()
      result
    }
  }
}
