package pool

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}
import java.util.function.BiFunction

class ConcurrentConnectionPool[Conn] {
  import ConcurrentConnectionPool._

  // TODO: Изчиствай неизползваните за определено време връзки
  private val pool = new ConcurrentHashMap[Host, ConcurrentLinkedQueue[Conn]]()

  def poll(host: Host): Option[Conn] = Option(pool.get(host)) flatMap { queue =>  Option(queue.poll()) }

  def offer(host: Host, conn: Conn) = {
    pool.compute(host, new BiFunction[Host, ConcurrentLinkedQueue[Conn], ConcurrentLinkedQueue[Conn]] {
      def apply(key: (String, Int), value: ConcurrentLinkedQueue[Conn]): ConcurrentLinkedQueue[Conn] = {
        val queue = if (value == null) new ConcurrentLinkedQueue[Conn] else value
        queue.offer(conn)
        queue
      }
    })
  }
}

object ConcurrentConnectionPool {
  type Host = (String, Int)
}
