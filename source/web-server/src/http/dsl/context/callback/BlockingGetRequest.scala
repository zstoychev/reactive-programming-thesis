package http.dsl.context.callback

import java.net.Socket
import java.nio.ByteBuffer

import http.Http._
import http.HttpResponseParser
import pool.ConcurrentConnectionPool
import reactor.util._

import scala.util.control.NonFatal

class BlockingGetRequest(connectionPool: ConcurrentConnectionPool[Socket])
                        (host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit) {
  def start(): Unit = {
    val socket = connectionPool.poll((host, port)) getOrElse new Socket(host, port)
    val inputStream = socket.getInputStream
    val outputStream = socket.getOutputStream

    try {
      val request = HttpRequest(HttpMethods.GET, path, Map(Headers.Host -> s"$host:$port"), HttpBody.Empty).toBytes
      outputStream.write(request)

      val buffer = new Array[Byte](1024)
      val byteBuffer = ByteBuffer.allocate(1024)
      val httpParser = new HttpResponseParser

      var ready = false

      while (!ready) {
        val size = inputStream.read(buffer)

        if (size == -1) {
          socket.close()
          handler(None)
          return
        }

        byteBuffer.fromByteArray(buffer)(size) {
          httpParser.receive(_) foreach { response =>
            connectionPool.offer((host, port), socket)
            handler(response.toOption)
            ready = true
          }
        }
      }
    } catch { case NonFatal(_) =>
      socket.close()
      handler(None)
    }
  }
}
