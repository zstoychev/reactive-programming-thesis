package proactor.nio

import java.net.{StandardSocketOptions, InetSocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousSocketChannel, CompletionHandler, AsynchronousServerSocketChannel}
import java.util.concurrent.{TimeUnit, ForkJoinPool}

class AsyncAcceptor(serverChannel: AsynchronousServerSocketChannel)(setupHandler: AsynchronousSocketChannel => Unit ) {
  serverChannel.accept((), new CompletionHandler[AsynchronousSocketChannel, Unit] {
    def completed(channel: AsynchronousSocketChannel, attachment: Unit): Unit = {
      channel.setOption[java.lang.Boolean](StandardSocketOptions.TCP_NODELAY, true)
      setupHandler(channel)
      serverChannel.accept((), this)
    }

    def failed(exc: Throwable, attachment: Unit): Unit = serverChannel.close()
  })
}

class AsyncEchoHandler(channel: AsynchronousSocketChannel) {
  private val buffer = ByteBuffer.allocate(8096)
  read()

  private object readHandler extends CompletionHandler[Integer, Unit] {
    def completed(readCount: Integer, attachment: Unit): Unit = {
      if (readCount == -1) channel.close()
      else write()
    }

    def failed(exc: Throwable, attachment: Unit): Unit = channel.close()
  }

  private object writeHandler extends CompletionHandler[Integer, Unit] {
    def completed(result: Integer, attachment: Unit): Unit = {
      buffer.compact()
      read()
    }

    def failed(exc: Throwable, attachment: Unit): Unit = channel.close
  }

  private def read(): Unit = channel.read(buffer, (), readHandler)
  private def write(): Unit = {
    buffer.flip()
    channel.write(buffer, (), writeHandler)
  }
}

object AsyncEchoServer {
  def main(args: Array[String]): Unit = {
    val asyncGroup = AsynchronousChannelGroup.withThreadPool(new ForkJoinPool)
    val serverChannel = AsynchronousServerSocketChannel.open(asyncGroup).bind(new InetSocketAddress(8000))

    new AsyncAcceptor(serverChannel)(new AsyncEchoHandler(_))

    asyncGroup.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
  }
}
