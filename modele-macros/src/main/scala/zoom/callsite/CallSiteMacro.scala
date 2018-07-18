package zoom

import java.io.File
import java.util.Date

import scala.io.Source
import scala.reflect.macros.Context

object CallSiteMacro {

  lazy val buildAt: Long = new Date().getTime

  def isClean(file: File): Boolean = true

  def commit(file: File): String = "no_commit"

  def pathToRepoRoot(file: File): String = file.getPath

  def fileContent(file: File): String = Source.fromFile(file).mkString("\n")

  def callSiteImpl(c: Context): c.Expr[Callsite] = {
    import c._
    import universe._

    val sourceFile = enclosingPosition.source.file.file

    //don't include the source in the compiled code if it's not needed
    val source: c.universe.Expr[Option[String]] = if (isClean(sourceFile))
      reify(None) else reify(Some(literal(fileContent(sourceFile)).splice))

    reify {
      Callsite(
        literal(enclosingClass.symbol.fullName).splice,
        literal(pathToRepoRoot(sourceFile)).splice,
        literal(enclosingPosition.line).splice,
        literal(commit(sourceFile)).splice,
        literal(buildAt).splice,
        literal(isClean(sourceFile)).splice,
        source.splice
      )
    }
  }
}
