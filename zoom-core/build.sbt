import sbt.{Project, RootProject}
import scalariform.formatter.preferences._

name := "zoom-core"

version := "1.0"

scalaVersion := "2.11.8"

val circeVersion = "0.8.0"
val scalaTestV = "3.0.1"

lazy val macros = RootProject(file("../modele-macros"))

lazy val main = Project(id = "zoom-core", base = file(".")).
  dependsOn(macros)

//Circe
libraryDependencies ++= Seq(
  "circe-core",
  "circe-generic",
  "circe-parser",
  "circe-generic-extras",
  "circe-optics"
).map(x => "io.circe" %% x % circeVersion)


libraryDependencies ++= Seq(
  //Kafka
  "org.apache.kafka" % "kafka-clients" % "0.11.0.0",
  "org.apache.kafka" % "kafka_2.11" % "0.11.0.0",

  //EmbeddedKafka
  "net.manub" %% "scalatest-embedded-kafka" % "0.15.1",

  //Test
  "org.scalatest" %% "scalatest" % scalaTestV % Test,

  "org.slf4j" % "slf4j-api" % "1.6.4",
  "org.slf4j" % "slf4j-log4j12" % "1.6.4",
  "org.json4s" %% "json4s-native" % "3.5.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.6.4",
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.9.2",
  "com.typesafe" % "config" % "1.3.2",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)


parallelExecution := false

packAutoSettings

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
