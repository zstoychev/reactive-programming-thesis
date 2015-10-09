package http.dsl.context.future

import java.nio.channels.SocketChannel
import java.util.concurrent.{TimeUnit, Executors}

import future.concurrent.{Promise, Future}
import http.Http.HttpResponse
import http.dsl.context.callback.HttpGetRequestHandler
import pool.ConcurrentConnectionPool
import reactor.basic.{FullReactor, AsyncOperationReactor, Reactor, TimeoutReactor}

import scala.concurrent.duration._

trait FutureBasedContext {
  def afterTimeout[T](timeout: Duration = 0.millis)(operation: => T): Future[T]
  def retrieve(host: String, port: Int, path: String): Future[HttpResponse]
}

class ConnectionException(message: String) extends Exception(message)

class ReactorContext(reactor: FullReactor) extends FutureBasedContext {
  import ReactorContext._

  def afterTimeout[T](timeout: Duration)(operation: => T) = {
    val p = Promise[T]
    reactor.afterTimeout(timeout)(p succeed operation)
    p.future
  }

  def retrieve(host: String, port: Int, path: String): Future[HttpResponse] = {
    val p = Promise[HttpResponse]
    new HttpGetRequestHandler(reactor)(connectionPool)(host, port, path)({
      case Some(response) => p succeed response
      case None => p fail new ConnectionException("Service unavailable or service error")
    })
    p.future
  }
}

object ReactorContext {
  val connectionPool = new ConcurrentConnectionPool[SocketChannel]
}