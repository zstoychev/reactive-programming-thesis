package parser

import scala.annotation.tailrec

trait Parser[I, O] { self =>
  private var currentState = process _
  private var pendingInput: Option[I] = None

  protected def process(input: I): Option[O]

  protected def become(state: I => Option[O]): Unit = currentState = state

  protected def becomeAndReceive(input: I)(state: I => Option[O]) = {
    become(state)
    pendingInput = Some(input)
    None
  }

  @tailrec
  final def receive(input: I): Option[O] = {
    val result = currentState(input)
    val moreInput = pendingInput
    pendingInput = None

    if (result.isDefined) result
    else moreInput match {
      case Some(input) => receive(input)
      case _ => None
    }
  }

  def >>[M](other: Parser[O, M]): Parser[I, M] = new Parser[I, M] {
    def process(input: I): Option[M] = self.receive(input) match {
      case Some(output) => other.receive(output)
      case None => None
    }
  }
}
