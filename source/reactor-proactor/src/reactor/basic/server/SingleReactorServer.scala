package reactor.basic.server

import java.net.InetSocketAddress
import java.nio.channels.{ServerSocketChannel, SocketChannel}

import reactor.basic.Reactor
import reactor.basic.acceptor.CommonAcceptor
import reactor.util._

class SingleReactorServer(port: Int)(handler: (Reactor, SocketChannel) => Unit) {
  def start() = {
    val reactor = new Reactor
    val serverSocket = ServerSocketChannel.open().nonblocking.bind(new InetSocketAddress(port))

    new CommonAcceptor(reactor, serverSocket)(handler)

    reactor.handleEvents()
  }
}
