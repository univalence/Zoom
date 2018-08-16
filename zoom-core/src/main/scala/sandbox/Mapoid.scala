package sandbox

/***
  * Mapoid are use to compensate magniola design
  */
sealed trait Mapoid

case class MLeaf(a: Any)                  extends Mapoid
case class MMap(map: Map[String, Mapoid]) extends Mapoid
case class MOption(opt: Option[Mapoid])   extends Mapoid

object Mapoid extends GenericShow[Mapoid] {

  def mleaf[T]: Typeclass[T] = MLeaf.apply

  implicit val str: Typeclass[String] = mleaf
  implicit val int: Typeclass[Int]    = mleaf

  implicit def opt[T: Typeclass]: Typeclass[Option[T]] =
    value ⇒ MOption(value.map(x ⇒ toMapoid(x)))

  override def join(typeName: String, strings: Seq[(String, Mapoid)]): Mapoid = MMap(strings.toMap)

  def toMapoid[T: Typeclass](t: T): Mapoid = implicitly[Typeclass[T]].show(t)
}
