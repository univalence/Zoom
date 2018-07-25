import sbt.{Project, RootProject}

import sbt.url

lazy val commonSettings = Seq(
  organization := "io.univalence",
  version := "0.3-SNAPSHOT",
  scalaVersion in ThisBuild := "2.11.12",
  parallelExecution := false,
  scalafmtOnCompile in ThisBuild := true,
  scalafmtTestOnCompile in ThisBuild := true
)

val libVersion = new {
  val circe     = "0.8.0"
  val scalaTest = "3.0.1"
  val kafka     = "0.11.0.0"
  val slf4j     = "1.6.4"
}

lazy val callsitemacro =
  (project in file("modele-macros"))
    .settings(commonSettings)
    .settings(
      libraryDependencies += "org.scala-lang"   % "scala-reflect"    % scalaVersion.value,
      libraryDependencies += "org.scalatest"    %% "scalatest"       % "3.0.5" % "test",
      libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.0.1.201806211838-r"
    )

lazy val core =
  (project in file("zoom-core"))
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "circe-core",
        "circe-generic",
        "circe-parser",
        "circe-generic-extras",
        "circe-optics"
      ).map(x => "io.circe" %% x % libVersion.circe),
      libraryDependencies ++= Seq(
        //Kafka
        "org.apache.kafka" % "kafka-clients" % libVersion.kafka,
        "org.apache.kafka" %% "kafka"        % libVersion.kafka,
        //EmbeddedKafka
        "net.manub" %% "scalatest-embedded-kafka" % "0.15.1",
        //Test
        "org.scalatest"          %% "scalatest"     % libVersion.scalaTest % Test,
        "org.slf4j"              % "slf4j-api"      % libVersion.slf4j,
        "org.slf4j"              % "slf4j-log4j12"  % libVersion.slf4j,
        "org.json4s"             %% "json4s-native" % "3.5.3",
        "org.scala-lang.modules" %% "scala-xml"     % "1.0.2",
        "com.sksamuel.avro4s"    %% "avro4s-core"   % "1.6.4",
        "joda-time"              % "joda-time"      % "2.9.9",
        "org.joda"               % "joda-convert"   % "1.9.2",
        "com.typesafe"           % "config"         % "1.3.2",
        "org.scala-lang"         % "scala-reflect"  % scalaVersion.value
      )
    )
    .dependsOn(callsitemacro)

lazy val root =
  (project in file("."))
    .settings(commonSettings)
    .settings(
      name := "zoom"
    )
    .aggregate(callsitemacro, core)

//TODO : Desactivate test in ||

licenses += "The Apache License, Version 2.0" ->
  url("http://www.apache.org/licenses/LICENSE-2.0.txt")

description := "Zoom is an event bus"

developers := List(
  Developer(
    id = "jwinandy",
    name = "Jonathan Winandy",
    email = "jonathan@univalence.io",
    url = url("https://github.com/ahoy-jon")
  ),
  Developer(
    id = "phong",
    name = "Philippe Hong",
    email = "philippe@univalence.io",
    url = url("https://github.com/hwki77")
  ),
  Developer(
    id = "fsarradin",
    name = "François Sarradin",
    email = "francois@univalence.io",
    url = url("https://github.com/fsarradin")
  )
)

scmInfo := Some(
  ScmInfo(
    url("https://github.com/UNIVALENCE/Zoom"),
    "scm:git:https://github.com/UNIVALENCE/Zoom.git",
    Some(s"scm:git:git@github.com:UNIVALENCE/Zoom.git")
  ))

publishMavenStyle := true
publishTo := Some(sonatypeDefaultResolver.value)

useGpg := true
