package zoom

case class Callsite(
  enclosingClass: String, //name of the enclosing unit
  file:           String, //file for the callsite (relative to the git root)
  line:           Int, //line in file
  commit:         String, //id of the commit
  buildAt:        Long, //time you build at
  clean:          Boolean, //if the built file is clean compare to git version
  fileContent:    Option[String] = None //if the build file is different
)

object Callsite {
  import language.experimental.macros

  implicit def callSite: Callsite = macro CallSiteMacro.callSiteImpl
}