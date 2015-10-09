package reactor

import reactor.basic.server.WorkersServer

object WorkersEchoServer {
  def main(args: Array[String]): Unit = new WorkersServer(8000)(new EchoHandler(_, _)).start()
}
