package sandbox

import shapeless._
import shapeless.labelled._

trait ToTypelessMap[T] {
  def toMap(t: T): Map[String, Any]
}

trait LowPriorityToMap2 {

  //CC to HList (LabelledGeneric)
  implicit def caseClassFields[F, G](implicit gen: LabelledGeneric.Aux[F, G],
                                     encode: ToTypelessMap[G]): ToTypelessMap[F] =
    (t: F) ⇒ encode.toMap(gen.to(t))

  implicit def hcons[K <: Symbol, Head, Tail <: HList](
      implicit key: Witness.Aux[K],
      tailToMap: ToTypelessMap[Tail]): ToTypelessMap[FieldType[K, Head] :: Tail] =
    (t: FieldType[K, Head] :: Tail) ⇒ {
      //can be changed to a typesafe version, with another typeclass to manage option...
      //for implementation sanity + compilation time ...
      t.head.asInstanceOf[Any] match {
        case Some(a) ⇒ tailToMap.toMap(t.tail).+(key.value.name → a)
        case None    ⇒ tailToMap.toMap(t.tail)
        case v       ⇒ tailToMap.toMap(t.tail).+(key.value.name → v)
      }

    }
}

object ToTypelessMap extends LowPriorityToMap2 {
  implicit val hnil: ToTypelessMap[HNil] = t ⇒ Map.empty

  implicit def opt[T: ToTypelessMap]: ToTypelessMap[Option[T]] =
    t ⇒ {
      t.fold(Map.empty[String, Any])(implicitly[ToTypelessMap[T]].toMap)
    }

  //Like hcons, but with a field that can turn into a map
  implicit def hconsRecur[K <: Symbol, Head, Tail <: HList](
      implicit key: Witness.Aux[K],
      headToMap: ToTypelessMap[Head],
      tailToMap: ToTypelessMap[Tail]): ToTypelessMap[FieldType[K, Head] :: Tail] =
    (t: FieldType[K, Head] :: Tail) ⇒ {
      val prefix = key.value.name
      headToMap.toMap(t.head).map({ case (k, v) ⇒ s"$prefix.$k" → v }) ++ tailToMap.toMap(t.tail)
    }

  def toMap[A](a: A)(implicit c: Cached[ToTypelessMap[A]]): Map[String, Any] = {
    c.value.toMap(a)
  }

  def apply[A: ToTypelessMap]: ToTypelessMap[A] = implicitly[ToTypelessMap[A]]
}
