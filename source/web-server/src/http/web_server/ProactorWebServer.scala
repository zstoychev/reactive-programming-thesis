package http.web_server

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels._
import java.util.concurrent.{ForkJoinPool, TimeUnit}

import http.HttpRequestParser
import http.dsl.CallbackBasedHttpDsL._
import http.dsl.HttpActions
import http.dsl.context.callback.{CallbackBasedContext, ProactorContext}
import proactor.nio.AsyncAcceptor
import reactor.util._

import scala.util.{Failure, Success}

class ProactorWebHandler(channel: AsynchronousSocketChannel)
                        (httpActions: HttpActions[CallbackBasedContext, AsyncActionResult]) {
  private val buffer = ByteBuffer.allocate(1024)
  private var outputBuffer: ByteBuffer = _
  private val httpParser = new HttpRequestParser

  read()

  private object readHandler extends CompletionHandler[Integer, Unit] {
    override def completed(readCount: Integer, attachment: Unit): Unit = {
      if (readCount == -1) channel.close()
      else {
        buffer.flip()
        val requestOpt = httpParser.receive(buffer)
        buffer.clear()
        requestOpt match {
          case Some(Success(request)) =>
            httpActions.execute(request)(ProactorContext) { response =>
              outputBuffer = response.toBytes.toByteBuffer
              write()
            }
          case Some(Failure(_)) => channel.close()
          case None => read()
        }
      }
    }

    override def failed(exc: Throwable, attachment: Unit): Unit = channel.close()
  }

  private object writeHandler extends CompletionHandler[Integer, Unit] {
    override def completed(result: Integer, attachment: Unit): Unit = {
      if (outputBuffer.hasRemaining) write()
      else read()
    }

    override def failed(exc: Throwable, attachment: Unit): Unit = channel.close
  }

  private def read(): Unit = channel.read(buffer, (), readHandler)
  private def write(): Unit = channel.write(outputBuffer, (), writeHandler)
}

object ProactorWebServer {
  def main(args: Array[String]): Unit = {
    val asyncGroup = AsynchronousChannelGroup.withThreadPool(new ForkJoinPool)
    val serverChannel = AsynchronousServerSocketChannel.open(asyncGroup).bind(new InetSocketAddress(8000))

    new AsyncAcceptor(serverChannel)(new ProactorWebHandler(_)(WebServerActions.actions))

    asyncGroup.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
  }
}

object SupportingProactorWebServer {
  def main(args: Array[String]): Unit = {
    val asyncGroup = AsynchronousChannelGroup.withThreadPool(new ForkJoinPool)
    val serverChannel = AsynchronousServerSocketChannel.open(asyncGroup).bind(new InetSocketAddress(8001))

    new AsyncAcceptor(serverChannel)(new ProactorWebHandler(_)(WebServerActions.actions))

    asyncGroup.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
  }
}
