package zoom

import shapeless.Id

import scala.language.higherKinds

/*
object TestModel {

  trait Logger[F[_]] {
    protected def log(str: ⇒ String, level: Level, callsite: Callsite): F[Unit]

    import Logger._

    def debug(str: ⇒ String)(implicit callsite: Callsite): F[Unit] = log(str, Debug, callsite)

    def info(str: ⇒ String)(implicit callsite: Callsite): F[Unit] = log(str, Info, callsite)

    def warn(str: ⇒ String)(implicit callsite: Callsite): F[Unit] = log(str, Warn, callsite)

    def error(str: ⇒ String)(implicit callsite: Callsite): F[Unit] = log(str, Error, callsite)
  }

  object Logger {

    object ops {
      implicit def toDslLogger[F[_]](o: Logger.type)(implicit m: Logger[F]): Logger[F] = m
    }

    def apply[F[_]](implicit m: Logger[F]): Logger[F] = m
  }

  trait EventSerializer[E] {

  }

  trait EventPublisher[F[_]] {
    def publish[E](e: E)(implicit eventSerializer: EventSerializer[E], callsite: Callsite): F[Unit]
  }

  object EventPublisher {

    object ops {
      implicit def toDslEventPublisher[F[_]](o: EventPublisher.type)(implicit m: EventPublisher[F]): EventPublisher[F] = m
    }

    def apply[F[_]](implicit m: EventPublisher[F]): EventPublisher[F] = m
  }

  import EventPublisher.ops._
  import Logger.ops._

  import scalaz._
  import syntax.bind._

  implicit val eventSerializer: EventSerializer[String] = new EventSerializer[String] {}

  def prog[F[_]: Monad: Logger: EventPublisher](x: String): F[Unit] = {

    for {
      _ ← Logger.info("test")
      _ ← Logger.warn("hello")
      _ ← EventPublisher.publish("abc")
    } yield {}
  }

  def main(args: Array[String]): Unit = {

    implicit val logger: Logger[Id] = new Logger[Id] {
      override protected def log(str: ⇒ String, level: Level, callsite: Callsite): Id[Unit] = {
        println("log : " + callsite + " - " + level + " - " + str)
      }
    }

    implicit val eventPublisher: EventPublisher[Id] = new EventPublisher[Id] {
      override def publish[E](e: E)(implicit eventSerializer: EventSerializer[E], callsite: Callsite): Id[Unit] = {
        println("publish : " + callsite + " - " + e)

      }
    }

    prog[Id]("yo")
  }

}
*/ 