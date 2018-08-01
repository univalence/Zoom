package zoom.test

import org.scalatest.{FunSuiteLike, Matchers}
import zoom.callsite.{CallSiteInfo, Implicit}

class TestCallSite extends FunSuiteLike with Matchers {

  val callsite: CallSiteInfo = Implicit.callSite

  test("callsite should on line it appears") {
    callsite.line should be(8)
  }
}
