package http.web_server

import java.io.IOException
import java.net.{ServerSocket, Socket}
import java.nio.ByteBuffer
import java.util.concurrent.{ThreadPoolExecutor, ExecutorService, Executors}

import http.HttpRequestParser
import http.dsl.CallbackBasedHttpDsL._
import http.dsl.HttpActions
import http.dsl.context.callback.{CallbackBasedContext, ThreadedContext}
import reactor.util._

import scala.util.{Failure, Success}

class SocketHandler(socket: Socket)
                   (httpActions: HttpActions[CallbackBasedContext, AsyncActionResult]) extends Runnable {
  def run(): Unit = {
    val buffer = Array.fill[Byte](64 * 1024)(0)
    val byteBuffer = ByteBuffer.allocate(64 * 1024)
    val httpParser = new HttpRequestParser

    val inputStream = socket.getInputStream
    val outputStream = socket.getOutputStream

    implicit val ctx = ThreadedContext

    try {
      while (true) {
        val size = inputStream.read(buffer)

        if (size == -1) { socket.close(); return }

        byteBuffer.fromByteArray(buffer)(size) {
          httpParser.receive(_) foreach {
            case Success(request) =>
              httpActions.execute(request)(ctx) { response =>
                val result = response.toBytes

                outputStream.write(result)
              }
            case Failure(_) =>
              socket.close()
              return
          }
        }
      }
    } catch { case e: IOException =>
      socket.close()
    }
  }
}

object ThreadedWebServer {
  val threadPool: ExecutorService = {
    val pool = Executors.newFixedThreadPool(1000).asInstanceOf[ThreadPoolExecutor]
    pool.prestartAllCoreThreads()
    pool
  }

  def main(args: Array[String]): Unit = {
    val socketServer = new ServerSocket(8000)

    while (true) {
      val socket = socketServer.accept()
      threadPool.submit(new SocketHandler(socket)(WebServerActions.actions))
    }
  }
}
