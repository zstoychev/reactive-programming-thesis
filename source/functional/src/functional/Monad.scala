package functional

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

trait Monad[F[_]] extends Applicative[F] {
  def flatMap[A, B](m: F[A])(f: A => F[B]): F[B]
  def unit[A](a: => A): F[A]

  def compose[A, B, C](f: A => F[B], g: B => F[C]): A => F[C] = a => flatMap(f(a))(g)

  def apply[A, B](mf: F[A => B])(ma: F[A]): F[B] = flatMap(mf)(f => map(ma)(f))
  override def map[A, B](m: F[A])(f: A => B): F[B] = flatMap(m)(x => unit(f(x)))
  override def map2[A, B, C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] = flatMap(ma)(a => map(mb)(b => f(a, b)))

  def join[A](mm: F[F[A]]): F[A] = flatMap(mm)(x => x)
}

object Monad {
  def compose[A, B, C, F[_]](f: A => F[B], g: B => F[C])(implicit m: Monad[F]) = m.compose(f, g)
  def map[A, B, F[_]](ma: F[A])(f: A => B)(implicit m: Monad[F]) = m.map(ma)(f)
  def map2[A, B, C, F[_]](ma: F[A], mb: F[B])(f: (A, B) => C)(implicit m: Monad[F]) = m.map2(ma, mb)(f)
  def join[A, F[_]](mm: F[F[A]])(implicit m: Monad[F]) = m.join(mm)
  def traverse[A, B, F[_]](xs: List[A])(f: A => F[B])(implicit m: Monad[F]) = m.traverse(xs)(f)
  def sequence[A, F[_]](ml: List[F[A]])(implicit m: Monad[F]) = m.sequence(ml)

  // Имплементация на монади в библиотеката на Scala
  implicit val optionMonad = new MonadWithZero[Option] {
    def flatMap[A, B](m: Option[A])(f: A => Option[B]): Option[B] = m flatMap f
    def unit[A](a: => A): Option[A] = Some(a)
    def mzero[A]: Option[A] = None

    override def map[A, B](m: Option[A])(f: A => B): Option[B] = m map f
  }

  implicit val tryMonad = new MonadWithZero[Try] {
    def flatMap[A, B](m: Try[A])(f: A => Try[B]): Try[B] = m flatMap f
    def unit[A](a: => A): Try[A] = Success(a)
    def mzero[A]: Try[A] = Failure(new NoSuchElementException)

    override def map[A, B](m: Try[A])(f: A => B): Try[B] = m map f
  }

  implicit val listMonad = new MonadWithZero[List] {
    def flatMap[A, B](m: List[A])(f: (A) => List[B]): List[B] = m flatMap f
    def unit[A](a: => A): List[A] = List(a)
    def mzero[A]: List[A] = Nil

    override def map[A, B](m: List[A])(f: A => B): List[B] = m map f
  }

  implicit def futureMonad(implicit ec: ExecutionContext) = new MonadWithZero[Future] {
    def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] = m flatMap f
    def unit[A](a: => A): Future[A] = Future(a)
    def mzero[A]: Future[A] = Future.failed(new NoSuchElementException)

    override def map[A, B](m: Future[A])(f: A => B): Future[B] = m map f
  }
}

trait MonadWithZero[F[_]] extends Monad[F] {
  def mzero[A]: F[A]
  def filter[A](m: F[A])(f: A => Boolean): F[A] = flatMap(m) { x => if (f(x)) unit(x) else mzero }
}
