package domainmodeling.healthcheck

import cats.free.*
import cats.implicits.*

object FreeMonadTest {

  trait DBOps[A]

  case class Create[A](key: String, value: A) extends DBOps[Unit]

  case class Read[A](key: String) extends DBOps[A]

  case class Update[A](key: String, value: A) extends DBOps[A]

  case class Delete(key: String) extends DBOps[Unit]

  type DBMonad[A] = Free[DBOps, A]

  def create[A](key: String, value: A): DBMonad[Unit] =
    Free.liftF[DBOps, Unit](Create(key, value))

  def get[A](key: String): DBMonad[A] =
    Free.liftF[DBOps, A](Read[A](key))

  def update[A](key: String, value: A): DBMonad[A] =
    Free.liftF[DBOps, A](Update[A](key, value))

  def delete(key: String): DBMonad[Unit] =
    Free.liftF(Delete(key))

  def f: String => String = ???

  def myLittleProgram: DBMonad[Unit] = for {
    _    <- create[String]("123-456", "Daniel")
    name <- get[String]("123-456")
    res  <- Free.pure(f(name))
    _    <- create[String]("567", name.toUpperCase())
    _    <- delete("123-456")
  } yield ()
}
