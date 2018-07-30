package sandbox

trait ToMapoid[T] {
  def toMap(t: T): Mapoid
}

sealed trait Mapoid

case class MLeaf(a: Any)                  extends Mapoid
case class MMap(map: Map[String, Mapoid]) extends Mapoid

object ToMapoid {
  def leaf[T]: ToMapoid[T] = {
    new ToMapoid[T] {
      override def toMap(t: T): Mapoid = MLeaf(t)
    }
  }

  implicit val stringMapoid: ToMapoid[String] = leaf[String]

  import language.experimental.macros, magnolia._

  type Typeclass[T] = ToMapoid[T]

  def combine[T](ctx: CaseClass[ToMapoid, T]): ToMapoid[T] = new Typeclass[T] {
    override def toMap(t: T): Mapoid =
      MMap(ctx.parameters.map { p ⇒
        p.label → p.typeclass.toMap(p.dereference(t))
      }.toMap)
  }

  //def dispatch[T](ctx: SealedTrait[ToMapoid, T]): ToMapoid[T] = ???

  implicit def gen[T]: ToMapoid[T] = macro Magnolia.gen[T]
}

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

    import ToMapoid._

    val toMapGen = gen[EmbedCaseClass]
    println(toMapGen.toMap(entity))
  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
