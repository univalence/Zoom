package zoom.util

import shapeless.labelled.FieldType
import shapeless._

object CCUtils {

  def rekey(str: String): String =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  def toMap[A](entity: A)(implicit A: ToMap[A]): Map[String, String] =
    A.toMap(entity).mapValues(_.toString)
}

trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

trait LowPriorityToMap {

  implicit def hcons[K <: Symbol, Head, Tail <: HList](implicit key: Witness.Aux[K],
                                                       tailToMap: ToMap[Tail]): ToMap[FieldType[K, Head] :: Tail] =
    new ToMap[FieldType[K, Head] :: Tail] {
      override def toMap(t: FieldType[K, Head] :: Tail): Map[String, Any] =
        t.head.asInstanceOf[Any] match {
          case Some(v) ⇒ tailToMap.toMap(t.tail) + (CCUtils.rekey(key.value.name) → v)
          case None    ⇒ tailToMap.toMap(t.tail)
          case v       ⇒ tailToMap.toMap(t.tail) + (CCUtils.rekey(key.value.name) → v)
        }
    }

}

object ToMap extends LowPriorityToMap {

  implicit def caseClassFields[F, G](implicit gen: LabelledGeneric.Aux[F, G], encode: ToMap[G]): ToMap[F] =
    new ToMap[F] {
      override def toMap(t: F): Map[String, Any] = encode.toMap(gen.to(t))
    }

  implicit val hnil: ToMap[HNil] =
    new ToMap[HNil] {
      override def toMap(t: HNil): Map[String, Any] = Map.empty
    }

  implicit def opt[T: ToMap]: ToMap[Option[T]] =
    new ToMap[Option[T]] {
      override def toMap(t: Option[T]): Map[String, Any] = t.fold(Map.empty[String, Any])(implicitly[ToMap[T]].toMap)
    }

  //Like hcons, but with a field that can turn into a map
  implicit def hconsRecur[K <: Symbol, Head, Tail <: HList](implicit key: Witness.Aux[K],
                                                            headToMap: ToMap[Head],
                                                            tailToMap: ToMap[Tail]): ToMap[FieldType[K, Head] :: Tail] =
    new ToMap[FieldType[K, Head] :: Tail] {
      override def toMap(t: FieldType[K, Head] :: Tail): Map[String, Any] = {
        val prefix = CCUtils.rekey(key.value.name)

        headToMap
          .toMap(t.head)
          .map {
            case (k, v) ⇒ s"$prefix.$k" → v
          } ++ tailToMap.toMap(t.tail)
      }
    }

  def toMap[A](a: A)(implicit c: ToMap[A]): Map[String, Any] = c.toMap(a)

  def apply[A: ToMap]: ToMap[A] = implicitly[ToMap[A]]

}
