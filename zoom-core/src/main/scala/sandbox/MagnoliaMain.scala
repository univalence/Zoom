package sandbox.testmagnolia



/******
  * START GENERIC MAGNIOLA PART
  */

import magnolia._
import shapeless.tag
import shapeless.tag.@@
import scala.reflect.runtime.universe.TypeTag


import scala.language.experimental.macros
import scala.reflect.ClassTag

trait Show[Out, T] { def show(value: T): Out }

trait GenericShow[Out] {
  type Typeclass[T] = Show[Out, T]

  //"simplifying" combine in the context of Show[Out,T]
  def join(typeName: String, params: Seq[(String, Out)]): Out

  //Default implementation for tuple go back to Case class
  def joinTuple(size:Int, params:Seq[Out]):Out = {
    join(s"Tuple$size", params.zipWithIndex.map({case (o,i) => {
      val n = i +1
      s"_$n" -> o
    }}))
  }

  def combine[T](ctx: CaseClass[Typeclass, T]): Show[Out, T] = value ⇒ {
    if(ctx.typeName.owner == "scala" && ctx.typeName.short.startsWith("Tuple")) {
      joinTuple(ctx.parameters.size,ctx.parameters.map(param => param.typeclass.show(param.dereference(value))))
    } else if (ctx.isValueClass) {
      val param = ctx.parameters.head
      param.typeclass.show(param.dereference(value))
    } else {
      val params: Seq[(String, Out)] = ctx.parameters.map { param ⇒
        param.label → param.typeclass.show(param.dereference(value))
      }
      join(ctx.typeName.short, params)
    }
  }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Show[Out, T] =
    value ⇒
      ctx.dispatch(value) { sub ⇒
        sub.typeclass.show(sub.cast(value))
      }

  implicit def gen[T]: Show[Out, T] = macro Magnolia.gen[T]
}

//DEMO HShow
class HShow(val str:String) extends AnyVal {
  override def toString: String = str
}

object HShow extends GenericShow[HShow] {
  private implicit def hShow(str:String):HShow = new HShow(str)

  implicit val int: Typeclass[Int] = _.toString
  implicit val str: Typeclass[String] =  x => "\"" + x + "\""

  implicit def opt[T:Typeclass]:Typeclass[Option[T]] = {
    case None => "None"
    case Some(x) => s"Option(${dump(x)}"
  }

  implicit val none:Typeclass[None.type] = _ => "None"

  implicit def seq[S[_]<:Seq[_],T](implicit SCT: ClassTag[S[_]], TTC:Typeclass[T]):Typeclass[S[T]] = s => {
    val seq = s.asInstanceOf[Seq[T]]
    val colName = SCT.runtimeClass.getSimpleName
    colName + seq.map(x => TTC.show(x)).mkString("(",", ",")")
  }

  override def joinTuple(size: Int, params: Seq[HShow]): HShow = {
    params.mkString("(",", ",")")
  }

  override def join(typeName: String, params: Seq[(String, HShow)]): HShow = {
    s"$typeName(${params.map({case (k,v) => s"$k = $v"}).mkString(",")})"
  }

  def dump[T:Typeclass](t:T):HShow = implicitly[Typeclass[T]].show(t)
}


object TestHShow {


  def main(args: Array[String]): Unit = {


    println(HShow.dump((1,2)))
    println(HShow.dump(SimpleCaseClass("abc")))
    println(HShow.gen[Tata].show(Tata(None)))
    println(HShow.dump(OffRoad(Some(Destination(1)))))

    Tata(toto = None)

    Toto(name = "toto",age = 13)
  }
}

case class Toto(name:String,age:Int)
case class Tata(toto:Option[Toto])

/*****
  * END GENERIC MAGNOLIA PART
  */


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


/***
  *
  * ToMap, the real deal !!
  */

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

  implicit def toMapCC[A <: Product: Mapoid.Typeclass]: ToMap[A] = (t: A) => {
    implicitly[Mapoid.Typeclass[A]].show(t) match {
      case mmap: MMap ⇒ mmapToMapString(mmap)
      case _ ⇒ throw new IllegalStateException("should not be possible, how did you do it ? #sarcasm")
    }
  }

  def toMap[A: ToMap](a: A): Map[String, Any] = implicitly[ToMap[A]].toMap(a)
}




sealed trait Path[+A]
case class Destination[+A](value: A)                    extends Path[A]
case class Crossroad[+A](left: Path[A], right: Path[A]) extends Path[A]
case class OffRoad[+A](path: Option[Path[A]])           extends Path[A]

object Path {

  def main(args: Array[String]): Unit = {
    val x: Path[Int] = OffRoad(Some(Destination(1)))
    println(Mapoid.toMapoid(x))
  }

}


object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

    println(ToMap.toMap(entity))

    println(ToMap.toMap(WithOption(entity, None)))

    val entity2 = WithOptionCC("", None, None)

    Mapoid.gen[WithOptionCC]
    println(Mapoid.toMapoid(entity2))

  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
case class WithOption(field: EmbedCaseClass, opt: Option[String])
case class WithOptionCC(field: String, opt: Option[String], opt2: Option[SimpleCaseClass])
