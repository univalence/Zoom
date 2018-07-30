package sandbox

trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

//object ToMap {
//
//  import language.experimental.macros, magnolia._
//
//  type Typeclass[T] = ToMap[T]
//
//  def combine[T](ctx: CaseClass[ToMap, T]): ToMap[T] =
//    (value: T) ⇒ {
//      ctx.parameters.flatMap { p ⇒
//        val subMap = p.typeclass.toMap(p.dereference(value))
//        subMap.map { case (k, v) ⇒ (k + "." + p.label) → v }
//      }.toMap
//    }
//
//  def dispatch[T](ctx: SealedTrait[ToMap, T]): ToMap[T] =
//    (value: T) ⇒
//      ctx.dispatch(value) { sub ⇒
//        sub.typeclass.toMap(sub.cast(value))
//    }
//
////  implicit val string: ToMap[String]
//
//  implicit def gen[T]: ToMap[T] = macro Magnolia.gen[T]
//
//}

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

//    val toMapGen = ToMap.gen[EmbedCaseClass]
//    println(toMapGen.toMap(entity))
  }

//  def mapFrom[A <: Product, L <: HList](entity: A)(implicit gen: LabelledGeneric.Aux[A, L],
//                                                   tmr: ToMap[L]): Map[String, Any] = {
//    val m: Map[tmr.Key, tmr.Value] = tmr(gen.to(entity))
//    m.flatMap {
//      case (k: Symbol, v: Product) ⇒
//        val subMap =
//          subMap.map { case (sk, sv) ⇒ (k.name + "." + sk) → v }
//      case (k: Symbol, v) ⇒ Map(k.name → v)
//    }
//  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
