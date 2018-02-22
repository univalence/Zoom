
import sbt.url

version := "0.3-SNAPSHOT"

name := "zoom"

scalaVersion := "2.11.8"

organization := "io.univalence"

lazy val callsitemacro = project in file("modele-macros")

lazy val core = (project in file("zoom-core")).dependsOn(callsitemacro)

lazy val root = (project in file(".")).aggregate(callsitemacro, core)


import scalariform.formatter.preferences._

scalariformPreferences := scalariformPreferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(IndentSpaces, 2)
  .setPreference(SpaceBeforeColon, false)
  .setPreference(CompactStringConcatenation, false)
  .setPreference(PreserveSpaceBeforeArguments, false)
  .setPreference(AlignParameters, true)
  .setPreference(AlignArguments, false)
  .setPreference(DoubleIndentConstructorArguments, false)
  .setPreference(FormatXml, true)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 50)
  .setPreference(IndentLocalDefs, false)
  .setPreference(DanglingCloseParenthesis, Force)
  .setPreference(SpaceInsideParentheses, false)
  .setPreference(SpaceInsideBrackets, false)
  .setPreference(SpacesWithinPatternBinders, true)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  .setPreference(IndentWithTabs, false)
  .setPreference(CompactControlReadability, false)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(SpacesAroundMultiImports, true)




licenses += "The Apache License, Version 2.0" ->
  url("http://www.apache.org/licenses/LICENSE-2.0.txt")

description := "Zoom is an event bus"

developers := List(
  Developer(
    id="jwinandy",
    name="Jonathan Winandy",
    email="jonathan@univalence.io",
    url=url("https://github.com/ahoy-jon")
  ),
  Developer(
    id="phong",
    name="Philippe Hong",
    email="philippe@univalence.io",
    url=url("https://github.com/hwki77")
  )
)

scmInfo := Some(ScmInfo(
  url("https://github.com/UNIVALENCE/Zoom"),
  "scm:git:https://github.com/UNIVALENCE/Zoom.git",
  Some(s"scm:git:git@github.com:UNIVALENCE/Zoom.git")
))


publishMavenStyle := true
publishTo := Some(sonatypeDefaultResolver.value)

useGpg := true
