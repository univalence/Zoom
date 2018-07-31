package sandbox

trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

object ToMap {
  private def mmapToMapString(mmap: MMap): Map[String, Any] = {
    mmap.map.flatMap({
      case (k, v) ⇒
        val res: Map[String, Any] = v match {
          case mmap2: MMap             ⇒ mmapToMapString(mmap2).map({ case (k2, v2) ⇒ s"$k.$k2" → v2 })
          case MOption(None)           ⇒ Map.empty
          case MOption(Some(s: MMap))  ⇒ mmapToMapString(s)
          case MOption(Some(MLeaf(a))) ⇒ Map(k → a)
          case MLeaf(a)                ⇒ Map(k → a)
        }
        res
    })
  }

  implicit def toMapCC[A <: Product: ToMapoid]: ToMap[A] = new ToMap[A] {
    override def toMap(t: A): Map[String, Any] = {
      implicitly[ToMapoid[A]].toMapoid(t) match {
        case mmap: MMap ⇒ mmapToMapString(mmap)
        case _          ⇒ throw new IllegalStateException("should not be possible, how did you do it ? #sarcasm")
      }
    }
  }

  def toMap[A: ToMap](a: A): Map[String, Any] = implicitly[ToMap[A]].toMap(a)
}

trait NotAProduct[T]

object NotAProduct {
  implicit def any[T]: NotAProduct[T]           = null
  implicit def p1[T <: Product]: NotAProduct[T] = null
  implicit def p2[T <: Product]: NotAProduct[T] = null
}

trait ToMapoid[T] {
  def toMapoid(t: T): Mapoid
}

sealed trait Mapoid

case class MLeaf(a: Any)                  extends Mapoid
case class MMap(map: Map[String, Mapoid]) extends Mapoid
case class MOption(opt: Option[Mapoid])   extends Mapoid

trait LowPriorityToMapoid {}

object ToMapoid extends LowPriorityToMapoid {

  type Typeclass[T] = ToMapoid[T]

  /*
  implicit def leaf[T: NotAProduct]: Typeclass[T] = {
    new ToMapoid[T] {
      override def toMapoid(t: T): Mapoid = MLeaf(t)
    }
  }
   */

  implicit val string: ToMapoid[String] = new Typeclass[String] {
    override def toMapoid(t: String): Mapoid = MLeaf(t)
  }

  implicit def opt[T](implicit lOpt: Typeclass[T]): Typeclass[Option[T]] = new Typeclass[Option[T]] {
    override def toMapoid(t: Option[T]): Mapoid = {
      MOption(t.map(lOpt.toMapoid))
    }
  }

  import language.experimental.macros, magnolia._

  def combine[T](ctx: CaseClass[ToMapoid, T]): ToMapoid[T] = new Typeclass[T] {
    override def toMapoid(t: T): Mapoid =
      MMap(ctx.parameters.map { p ⇒
        p.label → p.typeclass.toMapoid(p.dereference(t))
      }.toMap)
  }

  //def dispatch[T](ctx: SealedTrait[ToMapoid, T]): ToMapoid[T] = ???

  implicit def gen[T]: ToMapoid[T] = macro Magnolia.gen[T]

  def apply[T: ToMapoid]: ToMapoid[T] = implicitly[ToMapoid[T]]

  def toMapoid[T: ToMapoid](t: T): Mapoid = implicitly[ToMapoid[T]].toMapoid(t)
}

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

    println(ToMap.toMap(entity))

    println(ToMap.toMap(WithOption(entity, None)))

    val entity2 = WithOptionCC("", None, None)

    // // TODO : Don't compile, bug in magnolia ?
    //ToMapoid.gen[WithOption]
    //println(ToMapoid.toMapoid(entity2))

  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
case class WithOption(field: EmbedCaseClass, opt: Option[String])
case class WithOptionCC(field: String, opt: Option[String], opt2: Option[SimpleCaseClass])
