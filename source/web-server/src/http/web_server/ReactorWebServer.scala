package http.web_server

import java.nio.channels.{SelectionKey, SocketChannel}

import http.HttpRequestParser
import http.dsl.CallbackBasedHttpDsL._
import http.dsl.HttpActions
import http.dsl.context.callback.{CallbackBasedContext, ReactorContext}
import pool.ArrayBuffer
import reactor.basic._
import reactor.basic.server.WorkersServer

import scala.util.{Failure, Success}

class WebHandler(reactor: FullReactor, channel: SocketChannel)
                (httpActions: HttpActions[CallbackBasedContext, AsyncActionResult])
    extends Handler with CloseOnIOError {
  val key = reactor.registerHandler(channel, SelectionKey.OP_READ, this)
  private var output: Option[ArrayBuffer] = None

  private val httpParser = new HttpRequestParser

  implicit private val ctx: CallbackBasedContext = new ReactorContext(reactor)

  override def read(): Unit = safe {
    reactor.byteBufferPool.withBuffer { inputBuffer =>
      val size = channel.read(inputBuffer)

      if (size == -1) { cleanup(); return }

      inputBuffer.flip()
      httpParser.receive(inputBuffer) foreach {
        case Success(request) =>
          key.interestOps(0)
          httpActions.execute(request)(ctx) { response =>
            output = Some((response.toBytes, 0))
            write()
          }
        case Failure(_) => channel.close()
      }
    }
  }

  override def write(): Unit = safe {
    output = reactor.byteBufferPool.withBuffer(output) { channel.write(_) }
    if (output.isDefined) key.interestOps(SelectionKey.OP_WRITE)
    else key.interestOps(SelectionKey.OP_READ)

  }
}

object ReactorWebServer {
  def main(args: Array[String]): Unit =
    new WorkersServer(8000)(new WebHandler(_, _)(WebServerActions.actions)).start()
}

object SupportingReactorWebServer {
  def main(args: Array[String]): Unit =
    new WorkersServer(8001)(new WebHandler(_, _)(WebServerActions.actions)).start()
}
