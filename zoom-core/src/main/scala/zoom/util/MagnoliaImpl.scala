package zoom.util

import magnolia._

import scala.collection.immutable.Map.Map1
import scala.language.experimental.macros

class Prefix private (private val names: String = "") extends AnyVal {

  def rekey(str: String): String =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  def add(name: String): Prefix =
    if (names.isEmpty) new Prefix(name)
    else new Prefix(names + "." + rekey(name))

  def toKey: String = names

}

object Prefix {
  def empty: Prefix = new Prefix()
}

trait ToMap[T] {

  def toMap(t: T, prefix: Prefix = Prefix.empty): Map[String, Any]
//  def toMap(t: T): Map[String, Any]

}

object ToMap {

  private val _instance: ToMap[Any] =
    new ToMap[Any] {
      override def toMap(t: Any, prefix: Prefix): Map[String, Any] =
        new Map1(prefix.toKey, t)
    }

  def instance[T]: ToMap[T] = _instance.asInstanceOf[ToMap[T]]

  implicit val str: ToMap[String] = instance
  implicit val int: ToMap[Int]    = instance

  implicit def opt[T](implicit T: ToMap[T]): ToMap[Option[T]] =
    new ToMap[Option[T]] {
      override def toMap(t: Option[T], prefix: Prefix): Map[String, Any] =
        t match {
          case None    ⇒ Map.empty
          case Some(x) ⇒ T.toMap(x, prefix)
        }
    }

  def toMap[A: ToMap](a: A): Map[String, Any] = implicitly[ToMap[A]].toMap(a)
}

trait MagnoliaContrat {

  type Typeclass[T]

  def combine[T](ctx: CaseClass[Typeclass, T]): Typeclass[T]

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T]

  implicit def gen[T]: ToMap[T] = macro Magnolia.gen[T]

}

object ToMapMagnolia extends MagnoliaContrat {

  override type Typeclass[T] = ToMap[T]

  override def combine[T](ctx: CaseClass[Typeclass, T]): ToMap[T] =
    new ToMap[T] {
      override def toMap(t: T, prefix: Prefix): Map[String, Any] = {
        ctx.parameters
          .flatMap(param ⇒ {
            param.typeclass.toMap(param.dereference(t), prefix.add(param.label))
          })
          .toMap
      }
    }

  override def dispatch[T](ctx: SealedTrait[Typeclass, T]): ToMap[T] =
    new ToMap[T] {
      override def toMap(t: T, prefix: Prefix): Map[String, Any] =
        ctx.dispatch(t) { sub ⇒
          sub.typeclass.toMap(sub.cast(t), prefix)
        }
    }

}
