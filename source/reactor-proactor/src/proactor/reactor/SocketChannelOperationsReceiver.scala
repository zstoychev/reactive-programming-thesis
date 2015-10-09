package proactor.reactor

trait SocketChannelOperationsReceiver {
  def startRead(): Unit
  def stopRead(): Unit
  def doWrite(data: Array[Byte]): Unit
}
