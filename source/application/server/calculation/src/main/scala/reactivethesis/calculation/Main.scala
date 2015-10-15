package reactivethesis.calculation

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

object Main extends App {
  val port = args match {
    case Array() => "0"
    case Array(port) => port
    case args =>
      println("Use single port parameter")
      sys.exit(1)
  }

  val newProperties = Map(
    "akka.remote.netty.tcp.port" -> port
  )
  val config = ConfigFactory.parseMap(newProperties).withFallback(ConfigFactory.load())

  val system = ActorSystem("application", config)

  new Calculation(system).start()
}
