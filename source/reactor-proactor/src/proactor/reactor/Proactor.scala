package proactor.reactor

import java.net.InetSocketAddress
import java.nio.channels.{ServerSocketChannel, SocketChannel}
import java.util.concurrent.atomic.AtomicInteger

import reactor.basic.FullReactor
import reactor.util._

class Proactor[CH](completionDispatcher: CompletionDispatcher[CH]) {
  private val acceptorReactor = new FullReactor
  private val numberOfCores = Runtime.getRuntime().availableProcessors()
  private val workerReactors = List.fill(numberOfCores)(new FullReactor)

  (acceptorReactor :: workerReactors).foreach { reactor =>
    new Thread(new Runnable {
      def run = reactor.handleEvents()
    }).start()
  }

  private val currentReactor = new AtomicInteger(0)
  private def nextReactor: FullReactor = workerReactors(math.abs(currentReactor.getAndIncrement()) % numberOfCores)

  def bind(address: InetSocketAddress, completionHandler: CH) = {
    val serverChannel = ServerSocketChannel.open().nonblocking.bind(address)
    acceptorReactor.receiveOperation(
      new ServerSocketChannelHandler(acceptorReactor, serverChannel, completionDispatcher, completionHandler))
  }

  def connect(address: InetSocketAddress, completionHandler: CH) = {
    val reactor = nextReactor

    reactor.receiveOperation {
      val channel = SocketChannel.open().nonblocking.noTcpDelay
      channel.connect(address)

      new SocketChannelHandler(reactor, channel, completionDispatcher, completionHandler)
    }
  }

  def handle(channel: SocketChannel, completionHandler: CH): Unit = {
    val reactor = nextReactor
    reactor.receiveOperation(new SocketChannelHandler(reactor, channel, completionDispatcher, completionHandler))
  }

  def stop() = (acceptorReactor :: workerReactors) foreach { _.stop() }
}
