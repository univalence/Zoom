package zoom.test

import org.scalatest.FunSuite
import zoom.Callsite

class TestCallSite extends FunSuite {

  val callsite: Callsite = Callsite.callSite

  test("hello callsite") {

    assert(callsite.line == 8)
  }
}