package parser

import java.nio.ByteBuffer

import scala.collection.mutable

class LineParser extends Parser[ByteBuffer, String] {
  val currentLine = mutable.ArrayBuilder.make[Byte]()
  currentLine.sizeHint(1024)

  protected def process(input: ByteBuffer): Option[String] = {
    while (input.hasRemaining) {
      val next = input.get()
      if (next == '\n') {
        val result = Some(new String(currentLine.result()))
        currentLine.clear()
        return result
      }
      else if (next != '\r') currentLine += next
    }

    None
  }
}
