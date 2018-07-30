package sandbox

import shapeless._
import shapeless.labelled._

import scala.language.implicitConversions

trait ToMap2[T] {
  def toMap(t: T): Map[String, Any]
}

trait LowPriorityToMap2 {

  implicit def caseClassFields[F, G](implicit gen: LabelledGeneric.Aux[F, G], encode: ToMap2[G]): ToMap2[F] =
    new ToMap2[F] {
      override def toMap(t: F): Map[String, Any] = encode.toMap(gen.to(t))
    }

  implicit def hcons[K <: Symbol, Head, Tail <: HList](implicit key: Witness.Aux[K],
                                                       tailToMap2: ToMap2[Tail]): ToMap2[FieldType[K, Head] :: Tail] =
    new ToMap2[FieldType[K, Head] :: Tail] {
      override def toMap(t: FieldType[K, Head] :: Tail): Map[String, Any] = {
        tailToMap2.toMap(t.tail).+(key.value.name → t.head)
      }
    }
}

object ToMap2 extends LowPriorityToMap2 {
  implicit val hnil: ToMap2[HNil] = new ToMap2[HNil] {
    override def toMap(t: HNil): Map[String, Any] = Map.empty
  }

  implicit def hconsRecur[K <: Symbol, Head, Tail <: HList](
      implicit key: Witness.Aux[K],
      hToMap2: ToMap2[Head],
      tailToMap2: ToMap2[Tail]): ToMap2[FieldType[K, Head] :: Tail] =
    new ToMap2[FieldType[K, Head] :: Tail] {
      override def toMap(t: FieldType[K, Head] :: Tail): Map[String, Any] = {
        val n = key.value.name
        hToMap2.toMap(t.head).map({ case (k, v) ⇒ s"$n.$k" → v }) ++ tailToMap2.toMap(t.tail)
      }
    }

  def toMap2[A: ToMap2](a: A): Map[String, Any] = {
    implicitly[ToMap2[A]].toMap(a)
  }
}

object ShapelessMain {

  def main(args: Array[String]): Unit = {
    println(ToMap2.toMap2(Whatever1("hello")))
    println(ToMap2.toMap2(Whatever2(Whatever1("world"))))
  }

}

case class Whatever1(value: String)

case class Whatever2(value: Whatever1)
