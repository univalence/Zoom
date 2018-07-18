package zoom.callsite

import zoom.CallSiteMacro

class CallSiteMacroTest extends org.scalatest.FunSuite {

  import CallSiteMacro._

  test("buildAt") {
    assert(buildAt <= System.currentTimeMillis())
  }

  test("is clean") {

    //TODO
    //Create a git root
    //Create a file
    //check not clean
    //commit the file
    //check is clean

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
