package reactor.basic.server

import java.net.InetSocketAddress
import java.nio.channels.{ServerSocketChannel, SocketChannel}

import reactor.basic._
import reactor.basic.acceptor.WorkersAcceptor
import reactor.util._

class WorkersServer(port: Int)
                   (handler: (FullReactor, SocketChannel) => Unit) {
  def start() = {
    val acceptorReactor = new Reactor with AsyncOperationReactor
    val numberOfCores = Runtime.getRuntime().availableProcessors()
    val workerReactors = List.fill(numberOfCores)(new FullReactor)

    val serverSocket = ServerSocketChannel.open().nonblocking.bind(new InetSocketAddress(port))

    acceptorReactor.receiveOperation(new WorkersAcceptor(acceptorReactor, workerReactors, serverSocket)(handler))

    (acceptorReactor :: workerReactors).foreach { reactor =>
      new Thread(new Runnable {
        def run = reactor.handleEvents()
      }).start()
    }
  }
}
