package http.web_server

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, SocketChannel}

import http.HttpRequestParser
import http.dsl.CallbackBasedHttpDsL._
import http.dsl.HttpActions
import http.dsl.context.callback.{CallbackBasedContext, ReactorContext}
import reactor.basic._
import reactor.basic.server.WorkersServer
import reactor.util._

import scala.util.{Failure, Success}

class AlternativeWebHandler(reactor: FullReactor, channel: SocketChannel)
                (httpActions: HttpActions[CallbackBasedContext, AsyncActionResult])
  extends Handler with CloseOnIOError {
  val key = reactor.registerHandler(channel, SelectionKey.OP_READ, this)
  private val inputBuffer = ByteBuffer.allocate(1024)
  private var outputBuffer: ByteBuffer = _

  private val httpParser = new HttpRequestParser

  implicit private val ctx: CallbackBasedContext = new ReactorContext(reactor)

  override def read(): Unit = safe {
    val size = channel.read(inputBuffer)

    if (size == -1) { cleanup(); return }

    inputBuffer.flip()
    httpParser.receive(inputBuffer) foreach {
      case Success(request) =>
        key.interestOps(0)
        httpActions.execute(request)(ctx) { response =>
          outputBuffer = response.toBytes.toByteBuffer
          reactor.receiveOperation(key.interestOps(SelectionKey.OP_WRITE))
        }
      case Failure(_) => channel.close()
    }
    inputBuffer.clear()
  }

  override def write(): Unit = safe {
    channel.write(outputBuffer)
    if (!outputBuffer.hasRemaining) key.interestOps(SelectionKey.OP_READ)
  }
}

object AlternativeReactorWebServer {
  def main(args: Array[String]): Unit =
    new WorkersServer(8000)(new AlternativeWebHandler(_, _)(WebServerActions.actions)).start()
}
