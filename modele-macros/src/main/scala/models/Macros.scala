package zoom

import scala.reflect.macros.Context

case class Callsite(
  enclosingClass:  String,      //name of the enclosing unit
  file:            String,     //file for the callsite
  line:            Int,       //line in file
  commit:          String,   //id of the commit
  buildAt:         Long,    //time you build at
  clean:           Boolean //if the buildfile is clean compare to git version
)

object Callsite {
  import language.experimental.macros

  implicit def callSite: Callsite = macro CallSiteMacro.callSiteImpl
}

object CallSiteMacro {


  lazy val buildAt:Long = new Date().getTime

  def isClean(file:File):Boolean = ???

  def commit(file:File):String = ???

  def pathToRepoRoot(file:File):String = ???


  def callSiteImpl(c: Context): c.Expr[Callsite] = {
    import c._
    import universe._


    val sourceFile = enclosingPosition.source.file

    reify {
      Callsite(
        literal(enclosingClass.symbol.fullName).splice,
        literal(pathToRepoRoot(sourceFile)).splice,
        literal(enclosingPosition.line).splice,
        literal(commit(sourceFile)).splice,
        literal(buildAt).splice,
        literal(isClean(sourceFile)).splice
      )
    }
  }
}