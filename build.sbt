import scalariform.formatter.preferences._

name := "wise-demo"

organization := "io.wisesystems"

version := "1.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %%  "akka-actor"               % "2.3.7",
  "com.typesafe.akka"          %% "akka-stream-experimental"  % "1.0-M2",
  "io.scalac"                  %%  "reactive-rabbit"          % "0.2.1",
  "com.typesafe.scala-logging" %%  "scala-logging-slf4j"      % "2.1.2",
  "ch.qos.logback"             %   "logback-core"             % "1.1.2",
  "ch.qos.logback"             %   "logback-classic"          % "1.1.2",
  "org.scalatest"              %%  "scalatest"                % "2.2.1" % "test"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
