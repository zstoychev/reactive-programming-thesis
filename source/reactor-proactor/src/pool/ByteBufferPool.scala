package pool

import java.nio.ByteBuffer

import scala.annotation.tailrec
import scala.collection.immutable.Queue

class ByteBufferPool(bufferSize: Int, poolSize: Int) {
  private var pool = Queue.fill(poolSize)(ByteBuffer.allocateDirect(bufferSize))

  def poll() = {
    val (buffer, newPool) = pool.dequeueOption getOrElse (ByteBuffer.allocate(bufferSize), pool)
    pool = newPool

    buffer
  }

  def offer(buffer: ByteBuffer) =
    if (buffer.isDirect && pool.size < poolSize) {
      buffer.clear()
      pool = pool enqueue buffer
    }

  def withBuffer(f: ByteBuffer => Unit) = {
    val buffer = poll()
    try f(buffer)
    finally offer(buffer)
  }

  def withBuffer(outputOpt: Option[ArrayBuffer])(f: ByteBuffer => Unit): Option[ArrayBuffer] = outputOpt match {
    case Some(output) =>
      val buffer = poll()

      @tailrec
      def write(output: ArrayBuffer): Option[ArrayBuffer] = output match { case (bytes, position) =>
        val length = math.min(bytes.length - position, buffer.remaining)
        buffer.clear()
        buffer.put(bytes, position, length)
        buffer.flip()

        f(buffer)
        val newPosition = position + length - buffer.remaining

        if (newPosition < bytes.length && newPosition != position) write((bytes, newPosition))
        else if (newPosition < bytes.length) Some((bytes, newPosition))
        else None
      }

      try write(output)
      finally offer(buffer)
    case None =>
      withBuffer(f)
      None
  }
}
