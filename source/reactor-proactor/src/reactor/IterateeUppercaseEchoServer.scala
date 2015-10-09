package reactor

import play.api.libs.iteratee.{CharEncoding, Concurrent, Enumeratee}
import reactor.basic.server.WorkersServer

import scala.concurrent.ExecutionContext.Implicits.global

object IterateeUppercaseEchoServer {
  def main (args: Array[String]): Unit = {
    new WorkersServer(8000)((reactor, channel) => {
      val (in, inputEnum) = Concurrent.joined[Array[Byte]]
      val out = inputEnum &> CharEncoding.decode("UTF-8") &> Enumeratee.map(_.toUpperCase.getBytes("UTF-8"))
      new IterateeHandler(reactor, channel)(in, out)
    }).start()
  }
}
