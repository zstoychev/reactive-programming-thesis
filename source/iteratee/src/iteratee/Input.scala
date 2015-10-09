package iteratee

sealed trait Input[+I]
case class El[+I](value: I) extends Input[I]
case object Empty extends Input[Nothing]
case object EOF extends Input[Nothing]
