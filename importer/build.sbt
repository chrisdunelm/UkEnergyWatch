//import AssemblyKeys._
import ScalaxbKeys._

name := "importer"

version := "0.1"

// Main deps
libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.slf4j" % "slf4j-api" % "1.7.6",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
)

// Test deps
libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.h2database" % "h2" % "1.3.175" % "test"
)

//assemblySettings

scalaxbSettings

sourceGenerators in Compile <+= scalaxb in Compile

//testOptions in Test += Tests.Argument("-oF")
