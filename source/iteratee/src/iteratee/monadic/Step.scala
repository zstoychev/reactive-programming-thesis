package iteratee.monadic

import iteratee.Input

import scala.language.higherKinds

sealed trait Step[I, +O, F[_]]
case class Done[I, O, F[_]](value: O, rest: Input[I]) extends Step[I, O, F]
case class Error[I, O, F[_]](error: Throwable) extends Step[I, O, F]
case class Cont[I, O, F[_]](k: Input[I] => Iteratee[I, O, F]) extends Step[I, O, F]
