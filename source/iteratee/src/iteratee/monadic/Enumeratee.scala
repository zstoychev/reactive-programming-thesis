package iteratee.monadic

import scala.language.higherKinds

trait Enumeratee[U, V, F[_]] {
  def apply[O](iteratee: Iteratee[V, O, F]): Iteratee[U, Iteratee[V, O, F], F]
}
