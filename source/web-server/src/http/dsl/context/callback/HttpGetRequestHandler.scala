package http.dsl.context.callback

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, SocketChannel}

import http.Http._
import http.HttpResponseParser
import pool.{ArrayBuffer, ConcurrentConnectionPool}
import reactor.basic.{ByteBufferPoolReactor, CloseOnIOError, Handler, Reactor}
import reactor.util._

class HttpGetRequestHandler(reactor: Reactor with ByteBufferPoolReactor)
                           (connectionPool: ConcurrentConnectionPool[SocketChannel])
                           (host: String, port: Int, path: String)
                           (handler: Option[HttpResponse] => Unit) extends Handler with CloseOnIOError {
  private val channel = connectionPool.poll((host, port)) getOrElse {
    val result = SocketChannel.open().nonblocking.noTcpDelay
    result.connect(new InetSocketAddress(host, port))
    result
  }
  val key = reactor.registerHandler(
    channel, if (channel.isConnected) SelectionKey.OP_WRITE else SelectionKey.OP_CONNECT, this)

  private var output: Option[ArrayBuffer] = Some(
    HttpRequest(HttpMethods.GET, path, Map(Headers.Host -> s"$host:$port"), HttpBody.Empty).toBytes, 0)

  private val httpParser = new HttpResponseParser

  override def cleanup(): Unit = {
    super.cleanup()
    handler(None)
  }

  override def connect(): Unit = safe {
    if (channel.finishConnect()) key.interestOps(SelectionKey.OP_WRITE)
    else cleanup()
  }

  override def write(): Unit = safe {
    output = reactor.byteBufferPool.withBuffer(output) { channel.write(_) }
    if (!output.isDefined) key.interestOps(SelectionKey.OP_READ)
  }

  override def read(): Unit = safe {
    reactor.byteBufferPool.withBuffer { inputBuffer =>
      val size = channel.read(inputBuffer)

      if (size == -1) { cleanup(); return }

      inputBuffer.flip()
      httpParser.receive(inputBuffer) foreach { response =>
        connectionPool.offer((host, port), channel)
        key.interestOps(0)
        key.attach(null)
        handler(response.toOption)
      }
    }
  }
}
