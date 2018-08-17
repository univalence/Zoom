package sandbox.magnolia2

/******
  * START GENERIC MAGNIOLA PART
  */
import magnolia._

import scala.collection.immutable.Map.Map1
import scala.language.experimental.macros

class Prefix private (private val names: String = "") extends AnyVal {
  def add(name: String): Prefix = if (names.isEmpty) new Prefix(name) else new Prefix(names + "." + name)
  def toKey: String             = names
}

object Prefix {
  def empty: Prefix = new Prefix()
}

trait ToMap[T] {
  def toMap(t: T, prefix: Prefix = Prefix.empty): Map[String, Any]
}

object ToMap {

  private val _instance: ToMap[Any] = (t, prefix) ⇒ new Map1(prefix.toKey, t)
  def instance[T]: ToMap[T]         = _instance.asInstanceOf[ToMap[T]]

  implicit val str: ToMap[String] = instance
  implicit val int: ToMap[Int]    = instance

  implicit def opt[T](implicit T: ToMap[T]): ToMap[Option[T]] =
    (t, prefix) ⇒
      t match {
        case None    ⇒ Map.empty
        case Some(x) ⇒ T.toMap(x, prefix)
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

  override def combine[T](ctx: CaseClass[Typeclass, T]): ToMap[T] = (t, prefix) ⇒ {
    ctx.parameters
      .flatMap(param ⇒ {
        param.typeclass.toMap(param.dereference(t), prefix.add(param.label))
      })
      .toMap
  }

  override def dispatch[T](ctx: SealedTrait[Typeclass, T]): ToMap[T] =
    (t: T, prefix) ⇒
      ctx.dispatch(t) { sub ⇒
        sub.typeclass.toMap(sub.cast(t), prefix)
    }

}

sealed trait Path[+A]
case class Destination[+A](value: A)                    extends Path[A]
case class Crossroad[+A](left: Path[A], right: Path[A]) extends Path[A]
case class OffRoad[+A](path: Option[Path[A]])           extends Path[A]

object Path {

  def main(args: Array[String]): Unit = {
    val x: Path[Int] = OffRoad(Some(Destination(1)))

    import ToMapMagnolia._

    implicit val x2 = ToMapMagnolia.gen[Path[Int]]

    println(ToMap.toMap(x))
  }

}

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity: EmbedCaseClass = EmbedCaseClass(SimpleCaseClass("Hello"))

    import ToMapMagnolia.gen

    println(gen[EmbedCaseClass].toMap(entity))

    println(ToMap.toMap(WithOption(entity, None)))

    val entity2 = WithOptionCC("", None, None)

    println(ToMap.toMap(entity2))

  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
case class WithOption(field: EmbedCaseClass, opt: Option[String])
case class WithOptionCC(field: String, opt: Option[String], opt2: Option[SimpleCaseClass])
