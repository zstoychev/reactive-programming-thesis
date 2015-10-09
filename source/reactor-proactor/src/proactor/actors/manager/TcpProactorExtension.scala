package proactor.actors.manager

import akka.actor._
import proactor.reactor.Proactor

class TcpProactorExtensionImpl(system: ActorSystem) extends Extension {
  val tcpManager = system.actorOf(Props(new TcpManager))
}

object TcpProactorExtension extends ExtensionId[TcpProactorExtensionImpl] with ExtensionIdProvider {
  def createExtension(system: ExtendedActorSystem): TcpProactorExtensionImpl = new TcpProactorExtensionImpl(system)

  def lookup(): ExtensionId[_ <: Extension] = this
}
