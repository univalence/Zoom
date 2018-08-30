package zoom.test

import callsite.CallSiteInfo
import org.scalatest.{FunSuiteLike, Matchers}

class TestCallSite extends FunSuiteLike with Matchers {

  val callsite: CallSiteInfo = implicitly[CallSiteInfo]

  test("callsite should on line it appears") {
    callsite.line should be(8)
  }
}
