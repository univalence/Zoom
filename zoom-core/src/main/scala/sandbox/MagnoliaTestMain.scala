package sandbox
import scala.reflect.ClassTag

object MagnoliaTestMain {
  def main(args: Array[String]): Unit = {

    println(HShow.dump((1, 2)))
    println(HShow.dump(SimpleCaseClass("abc")))
    println(HShow.gen[Tata].show(Tata(None)))
    println(HShow.dump(OffRoad(Some(Destination(1)))))

    Tata(toto = None)

    Toto(name = "toto", age = 13)
  }

}

case class Toto(name: String, age: Int)
case class Tata(toto: Option[Toto])

//DEMO HShow
class HShow(val str: String) extends AnyVal {
  override def toString: String = str
}

object HShow extends GenericShow[HShow] {
  private implicit def hShow(str: String): HShow = new HShow(str)

  implicit val int: Typeclass[Int]    = _.toString
  implicit val str: Typeclass[String] = x ⇒ "\"" + x + "\""

  implicit def opt[T: Typeclass]: Typeclass[Option[T]] = {
    case None    ⇒ "None"
    case Some(x) ⇒ s"Option(${dump(x)}"
  }

  implicit val none: Typeclass[None.type] = _ ⇒ "None"

  implicit def seq[S[_] <: Seq[_], T](implicit SCT: ClassTag[S[_]], TTC: Typeclass[T]): Typeclass[S[T]] = s ⇒ {
    val seq     = s.asInstanceOf[Seq[T]]
    val colName = SCT.runtimeClass.getSimpleName
    colName + seq.map(x ⇒ TTC.show(x)).mkString("(", ", ", ")")
  }

  override def joinTuple(size: Int, params: Seq[HShow]): HShow = {
    params.mkString("(", ", ", ")")
  }

  override def join(typeName: String, params: Seq[(String, HShow)]): HShow = {
    s"$typeName(${params.map({ case (k, v) ⇒ s"$k = $v" }).mkString(",")})"
  }

  def dump[T: Typeclass](t: T): HShow = implicitly[Typeclass[T]].show(t)
}
