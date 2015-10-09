package http.dsl.context.callback

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, CompletionHandler}

import http.Http._
import http.HttpResponseParser
import pool.ConcurrentConnectionPool
import reactor.util._

class ProactorHttpGetRequestHandler(connectionPool: ConcurrentConnectionPool[AsynchronousSocketChannel])
                                   (host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit) {
  private val (channel, connected) = connectionPool.poll((host, port)) map { (_, true) } getOrElse
    (AsynchronousSocketChannel.open(), false)

  private val outputBuffer = HttpRequest(HttpMethods.GET, path,
    Map(Headers.Host -> s"$host:$port"), HttpBody.Empty).toBytes.toByteBuffer

  private val httpParser = new HttpResponseParser
  private val inputBuffer = ByteBuffer.allocate(1024)

  if (connected) write()
  else channel.connect(new InetSocketAddress(host, port), (), new CompletionHandler[Void, Unit] {
    def completed(result: Void, attachment: Unit): Unit = { write() }

    def failed(exc: Throwable, attachment: Unit): Unit = cleanup()
  })

  private def cleanup() = {
    channel.close()
    handler(None)
  }

  private object readHandler extends CompletionHandler[Integer, Unit] {
    override def completed(readCount: Integer, attachment: Unit): Unit = {
      if (readCount == -1) channel.close()
      else {
        inputBuffer.flip()
        val responseOpt = httpParser.receive(inputBuffer)
        responseOpt match {
          case Some(response) =>
            connectionPool.offer((host, port), channel)
            handler(response.toOption)
          case None => read()
        }
      }
    }

    override def failed(exc: Throwable, attachment: Unit): Unit = cleanup()
  }

  private object writeHandler extends CompletionHandler[Integer, Unit] {
    override def completed(result: Integer, attachment: Unit): Unit = {
      if (outputBuffer.hasRemaining) write()
      else read()
    }

    override def failed(exc: Throwable, attachment: Unit): Unit = cleanup()
  }

  private def read(): Unit = channel.read(inputBuffer, (), readHandler)
  private def write(): Unit = channel.write(outputBuffer, (), writeHandler)
}
