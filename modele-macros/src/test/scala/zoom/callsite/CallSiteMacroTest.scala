package zoom.callsite

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import zoom.{CallSiteMacro, Callsite, JGitTools}

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

    val root: Path = Files.createTempDirectory("testGit")

    val git = Git.init().setDirectory(root.toFile).call()


    val file = root.resolve("newFile").toFile

    file.createNewFile()
    git.add().addFilepattern("newFile")
    val rc = git.commit().setMessage("yolo").call()


    assert(commit(file) == rc.name())


    Files.write(file.toPath, "file contents".getBytes(StandardCharsets.UTF_8))

    assert(!isClean(file))
    assert(commit(file) == rc.name())

    val rc2 = git.commit().setAll(true).setMessage("yololo").call()

    assert(commit(file) == rc2.name())

  }

  test("pathToRoot") {
    val root: Path = Files.createTempDirectory("testGit")

    val git = Git.init().setDirectory(root.toFile).call()

    val dir = root.resolve("a/b/c/d").toFile
    dir.mkdirs()

    val file = root.resolve("a/b/c/d/e.txt").toFile
    file.createNewFile()


    assert(pathToRepoRoot(file) == "a/b/c/d/e.txt")

  }

  test("macro test") {

    val cs = implicitly[Callsite]


    val git = JGitTools.getGit(new File(".")).get


    assert(cs.commit == git.getRepository.resolve(Constants.HEAD).getName)

  }
}
