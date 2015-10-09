package reactor

import reactor.basic.server.SingleReactorServer

object EchoServer {
  def main (args: Array[String]): Unit = {
    new SingleReactorServer(8000)(new EchoHandler(_, _)).start()
  }
}
