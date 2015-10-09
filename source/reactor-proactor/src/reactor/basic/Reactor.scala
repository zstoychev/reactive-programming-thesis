package reactor.basic

import java.nio.channels.{SelectableChannel, SelectionKey, Selector}

import scala.collection.JavaConversions.asScalaSet
import scala.util.control.NonFatal

trait Handler {
  def key: SelectionKey

  def cleanup(): Unit = key.cancel()

  def accept(): Unit = ()
  def connect(): Unit = ()
  def read(): Unit = ()
  def write(): Unit = ()
}

trait CloseOnIOError extends Handler {
  private def closeOnError(code: => Any) = try code catch { case NonFatal(e) => cleanup() }

  abstract override def cleanup() = {
    super.cleanup()
    key.channel.close()
  }

  protected def safe(action: => Unit) = closeOnError(action)
}

class Reactor {
  protected val selector = Selector.open()
  @volatile private var exit = false

  protected def nextTimeout: Long = 0L

  protected def preSelectJobs: List[() => Unit] = List.empty

  def registerHandler(channel: SelectableChannel, operations: Int, handler: Handler): SelectionKey =
    channel.register(selector, operations, handler)

  def removeHandler(handler: Handler): Unit = try { handler.cleanup() } catch { case NonFatal(e) => }

  def stop() = {
    exit = true
    selector.wakeup()
  }

  def handleEvents(): Unit = {
    while (!exit) {
      preSelectJobs.foreach { _() }

      selector.select(nextTimeout)
      val selectedKeys = selector.selectedKeys()

      selectedKeys.foreach { key =>
        val handler = key.attachment.asInstanceOf[Handler]

        try {
          if (key.isValid && key.isAcceptable) handler.accept()
          if (key.isValid && key.isConnectable) handler.connect()
          if (key.isValid && key.isReadable) handler.read()
          if (key.isValid && key.isWritable) handler.write()
        } catch { case NonFatal(e) => removeHandler(handler) }
      }
      selectedKeys.clear()
    }

    selector.keys().map(_.attachment.asInstanceOf[Handler].cleanup())
    selector.close()
  }
}
