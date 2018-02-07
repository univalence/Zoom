package zoom

import scala.reflect.macros.Context

case class Callsite(
  enclosingClass:  String,
  enclosingMethod: Option[String],
  file:            String,
  line:            Int
)

//TO UPDATE
object Callsite {

  import language.experimental.macros

  implicit def callSite: Callsite = macro CallSiteMacro.callSiteImpl

  //def callSite:Callsite = Callsite("TestCallSite",None,"filename.scala",11)
}

object CallSiteMacro {
  def callSiteImpl(c: Context): c.Expr[Callsite] = {
    import c._
    import universe._

    val method =
      scala.util.Try(enclosingMethod.symbol.name.decoded).toOption match {
        case Some(s) ⇒ reify(Some(literal(s).splice))
        case None    ⇒ reify(None)
      }

    reify {
      Callsite(
        literal(enclosingClass.symbol.fullName).splice,
        method.splice,
        literal(enclosingPosition.source.file.absolute.path).splice,
        literal(enclosingPosition.line).splice
      )
    }
  }
}