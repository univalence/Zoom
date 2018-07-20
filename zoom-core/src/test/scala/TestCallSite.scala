package zoom.test

import org.scalatest.FunSuite
import zoom.callsite.{CallSiteInfo, Implicit}

class TestCallSite extends FunSuite {

  val callsite: CallSiteInfo = Implicit.callSite

  test("hello callsite") {

    assert(callsite.line == 8)
  }
}
