package zoom.callsite

import java.io.File

import org.eclipse.jgit.lib.Constants
import org.scalatest.{ FunSuiteLike, Matchers }

class CallSiteMacroTest extends FunSuiteLike with Matchers {

  import CallSiteMacro._
  import Implicit._

  test("should have builtAt lower or equal to the current time") {
    buildAt should be <= System.currentTimeMillis()
  }

  test("macro test") {
    val cs = implicitly[CallSiteInfo]
    val git = GitTools.getGit(new File(".")).get

    assert(cs.commit == git.getRepository.resolve(Constants.HEAD).getName)
  }
}
