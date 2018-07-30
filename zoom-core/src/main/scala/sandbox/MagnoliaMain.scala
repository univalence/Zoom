package sandbox

trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

object ToMap {
  private def mmapToMapString(mmap: MMap): Map[String, Any] = {
    mmap.map.flatMap({
      case (k, v) ⇒
        val res: Map[String, Any] = v match {
          case mmap2: MMap ⇒ mmapToMapString(mmap2).map({ case (k2, v2) ⇒ s"$k.$k2" → v2 })
          case MLeaf(a)    ⇒ Map(k → a)
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

trait ToMapoid[T] {
  def toMapoid(t: T): Mapoid
}

trait NotAProduct[T]

object NotAProduct {
  implicit def any[T]: NotAProduct[T]           = null
  implicit def p1[T <: Product]: NotAProduct[T] = null
  implicit def p2[T <: Product]: NotAProduct[T] = null
}

sealed trait Mapoid

case class MLeaf(a: Any)                  extends Mapoid
case class MMap(map: Map[String, Mapoid]) extends Mapoid

object ToMapoid {
  implicit def leaf[T: NotAProduct]: ToMapoid[T] = {
    new ToMapoid[T] {
      override def toMapoid(t: T): Mapoid = MLeaf(t)
    }
  }

  import language.experimental.macros, magnolia._

  type Typeclass[T] = ToMapoid[T]

  def combine[T](ctx: CaseClass[ToMapoid, T]): ToMapoid[T] = new Typeclass[T] {
    override def toMapoid(t: T): Mapoid =
      MMap(ctx.parameters.map { p ⇒
        p.label → p.typeclass.toMapoid(p.dereference(t))
      }.toMap)
  }

  //def dispatch[T](ctx: SealedTrait[ToMapoid, T]): ToMapoid[T] = ???

  implicit def gen[T]: ToMapoid[T] = macro Magnolia.gen[T]
}

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

    println(ToMap.toMap(entity))
  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
