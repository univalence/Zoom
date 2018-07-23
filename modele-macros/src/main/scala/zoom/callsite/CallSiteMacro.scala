package zoom.callsite

import java.io.File

import scala.io.Source
import scala.reflect.macros.blackbox

object CallSiteMacro {

  import GitTools._

  lazy val buildAt: Long = System.currentTimeMillis()

  def fileContent(file: File): String =
    Source.fromFile(file).mkString("\n")

  def callSiteImpl(c: blackbox.Context): c.Expr[CallSiteInfo] = {
    import c._
    import universe._

    val sourceFile = enclosingPosition.source.file.file

    //don't include the source in the compiled code if it's not needed
    val source: c.universe.Expr[Option[String]] =
      if (isClean(sourceFile))
        reify(None)
      else reify(Some(literal(fileContent(sourceFile)).splice))

    reify {
      CallSiteInfo(
        literal(enclosingClass.symbol.fullName).splice,
        literal(pathToRepoRoot(sourceFile)).splice,
        literal(enclosingPosition.line).splice,
        literal(lastCommitIdOf(sourceFile)).splice,
        literal(buildAt).splice,
        literal(isClean(sourceFile)).splice,
        source.splice
      )
    }
  }

}
