package iteratee.monadic

import scala.language.higherKinds

trait Enumerator[I, F[_]] {
  def apply[O](iteratee: Iteratee[I, O, F]): Iteratee[I, O, F]
}
