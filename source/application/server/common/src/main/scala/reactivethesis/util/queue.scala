package reactivethesis.util

import scala.collection.immutable.Queue

object queue {
  implicit class RichQueue[T](val queue: Queue[T]) extends AnyVal {
    def enqueueBounded(bound: Int)(element: T) = {
      queue.drop(Math.max(queue.size - bound - 1, 0)).enqueue(element)
    }

    def enqueueBoundedIfNotFull(bound: Int)(element: T) = {
      if (queue.size >= bound) queue else queue.enqueue(element)
    }
  }
}
