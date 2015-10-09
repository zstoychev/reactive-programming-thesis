package reactor.basic

import pool.ByteBufferPool

trait ByteBufferPoolReactor extends Reactor {
  import ByteBufferPoolReactor._
  val byteBufferPool = new ByteBufferPool(DefaultBufferSize, DefaultPoolSize)
}

object ByteBufferPoolReactor {
  val DefaultBufferSize = 64 * 1024
  val DefaultPoolSize = 8
}
