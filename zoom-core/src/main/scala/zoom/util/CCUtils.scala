package zoom.util

import shapeless._
import shapeless.labelled.FieldType

object CCUtils {

  def rekey(str: String): String =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  //FIXME : Rendre + générique !
  def getCCParams_old(entity: AnyRef): Map[String, String] =
    entity.getClass.getDeclaredFields
      .foldLeft(Map[String, String]()) { (a, field) ⇒
        field.setAccessible(true)

        val pair: Map[String, String] =
          field.get(entity) match {
            case Seq()           ⇒ Map.empty
            case Some(v: String) ⇒ Map(field.getName → v)
            case None            ⇒ Map.empty
            case Some(subref: AnyRef) ⇒
              val subMap = getCCParams_old(subref)
              subMap.map(sm ⇒ (field.getName + "." + sm._1) → sm._2)
            case _ ⇒ Map(field.getName → field.get(entity).toString)
          }

        a ++ pair
      }
      .map(t ⇒ rekey(t._1) → t._2)
      .filter(_._2.nonEmpty)

  def getCCParams[A](entity: A)(implicit A: ToMap[A]): Map[String, String] =
    A.toMap(entity).mapValues(_.toString)
}

trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

trait LowPriorityToMap {

  implicit def hcons[K <: Symbol, Head, Tail <: HList](implicit key: Witness.Aux[K],
                                                       tailToMap: ToMap[Tail]): ToMap[FieldType[K, Head] :: Tail] =
    (t: FieldType[K, Head] :: Tail) ⇒
      //can be changed to a typesafe version, with another typeclass to manage option...
      //for implementation sanity + compilation time ...
      t.head.asInstanceOf[Any] match {
        case Some(v) ⇒ tailToMap.toMap(t.tail) + (CCUtils.rekey(key.value.name) → v)
        case None    ⇒ tailToMap.toMap(t.tail)
        case v       ⇒ tailToMap.toMap(t.tail) + (CCUtils.rekey(key.value.name) → v)
    }

}

object ToMap extends LowPriorityToMap {

  implicit def caseClassFields[F, G](implicit gen: LabelledGeneric.Aux[F, G], encode: ToMap[G]): ToMap[F] =
    (t: F) ⇒ encode.toMap(gen.to(t))

  implicit val hnil: ToMap[HNil] = (t: HNil) ⇒ Map.empty

  implicit def opt[T: ToMap]: ToMap[Option[T]] =
    (t: Option[T]) ⇒ {
      t.fold(Map.empty[String, Any])(implicitly[ToMap[T]].toMap)
    }

  //Like hcons, but with a field that can turn into a map
  implicit def hconsRecur[K <: Symbol, Head, Tail <: HList](implicit key: Witness.Aux[K],
                                                            headToMap: ToMap[Head],
                                                            tailToMap: ToMap[Tail]): ToMap[FieldType[K, Head] :: Tail] =
    (t: FieldType[K, Head] :: Tail) ⇒ {
      val prefix = CCUtils.rekey(key.value.name)

      headToMap
        .toMap(t.head)
        .map {
          case (k, v) ⇒ s"$prefix.$k" → v
        } ++ tailToMap.toMap(t.tail)
    }

  def toMap[A](a: A)(implicit c: ToMap[A]): Map[String, Any] = c.toMap(a)

  def apply[A: ToMap]: ToMap[A] = implicitly[ToMap[A]]

}
