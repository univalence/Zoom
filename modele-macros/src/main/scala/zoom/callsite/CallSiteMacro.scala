package zoom

import java.io.File
import java.util.Date

import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.{Constants, Repository}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import scala.io.Source
import scala.reflect.macros.blackbox


object JGitTools {


  def getGit(file:File):Option[Git] = {
    val repositoryBuilder: FileRepositoryBuilder = new FileRepositoryBuilder()
    if(repositoryBuilder.findGitDir(file).getGitDir != null) {
      val r: Repository = repositoryBuilder.build()
      val git = Git.open(r.getDirectory)
      Some(git)
    } else {
      None
    }
  }



}

object CallSiteMacro {

  lazy val buildAt: Long = new Date().getTime


  private def pathInGit(file:File,git:Git):Option[String] = {
    val path = git.getRepository.getWorkTree.toPath
    Some(path.toRealPath().relativize(file.toPath.toRealPath()).toString)
  }

  import JGitTools._

  def isClean(file: File): Boolean = {
    (for {
      git <- getGit(file)
      path <- pathInGit(file,git)
    } yield {
      git.status().addPath(path).call().isClean
    }).getOrElse(false)
  }

  def commit(file: File): String = {
    getGit(file).flatMap(g => {
      Option(g.getRepository.resolve(Constants.HEAD))
    }).map(_.getName).getOrElse("")

  }

  def pathToRepoRoot(file: File): String = {
    getGit(file).flatMap(git => pathInGit(file,git)).getOrElse(file.getPath)
  }

  def fileContent(file: File): String = Source.fromFile(file).mkString("\n")

  def callSiteImpl(c: blackbox.Context): c.Expr[Callsite] = {
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
