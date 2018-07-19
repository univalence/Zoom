package zoom.callsite

import java.nio.file.{Files, Path}

import org.eclipse.jgit.api.Git
import zoom.CallSiteMacro

class CallSiteMacroTest extends org.scalatest.FunSuite {

  import CallSiteMacro._

  test("buildAt") {
    assert(buildAt <= System.currentTimeMillis())
  }

  test("is clean") {

    val root: Path = Files.createTempDirectory("testGit")

    val git = Git.init().setDirectory(root.toFile).call()

    val file = root.resolve("newFile").toFile

    assert(!file.exists())
    file.createNewFile()
    assert(file.exists())


    assert(!isClean(file))

    git.add().addFilepattern("newFile").call()

    git.commit().setMessage("yolo").call()

    assert(isClean(file))




  }

  test("commit") {

    //TODO
    //Create a git root
    //create a file
    //check commit
    //commit
    //check commit

  }

  test("pathToRoot") {

    //TODO
    //create a git root
    //create a file
    //check path to root

  }
}
