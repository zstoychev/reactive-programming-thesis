package http.dsl.context.callback

import java.net.Socket
import java.nio.channels.{AsynchronousSocketChannel, SocketChannel}
import java.util.concurrent.{TimeUnit, Executors}

import http.Http.HttpResponse
import pool.ConcurrentConnectionPool
import reactor.basic.{FullReactor, AsyncOperationReactor, Reactor, TimeoutReactor}

import scala.concurrent.duration._

trait CallbackBasedContext {
  def afterTimeout(timeout: Duration = 0.millis)(handler: => Unit): Unit
  def retrieve(host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit): Unit
}

class ReactorContext(reactor: FullReactor) extends CallbackBasedContext {
  import ReactorContext._

  def afterTimeout(timeout: Duration)(handler: => Unit): Unit = reactor.afterTimeout(timeout)(handler)

  def retrieve(host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit): Unit =
    new HttpGetRequestHandler(reactor)(connectionPool)(host, port, path)(handler)
}

object ReactorContext {
  val connectionPool = new ConcurrentConnectionPool[SocketChannel]
}

object ProactorContext extends CallbackBasedContext {
  private val scheduler = Executors.newScheduledThreadPool(1)
  val connectionPool = new ConcurrentConnectionPool[AsynchronousSocketChannel]

  def afterTimeout(timeout: Duration)(handler: => Unit): Unit = scheduler.schedule(new Runnable {
    def run = handler
  }, timeout.toMillis, TimeUnit.MILLISECONDS)

  def retrieve(host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit): Unit =
    new ProactorHttpGetRequestHandler(connectionPool)(host, port, path)(handler)
}

object ThreadedContext extends CallbackBasedContext {
  val connectionPool = new ConcurrentConnectionPool[Socket]

  def afterTimeout(timeout: Duration)(handler: => Unit): Unit = {
    Thread.sleep(timeout.toMillis)
    handler
  }

  def retrieve(host: String, port: Int, path: String)(handler: Option[HttpResponse] => Unit): Unit =
    new BlockingGetRequest(connectionPool)(host, port, path)(handler).start()
}
