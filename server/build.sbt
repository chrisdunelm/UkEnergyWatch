
lazy val root = (project in file("."))
  .aggregate(utils)
  .aggregate(db)
  .aggregate(data)
  .aggregate(importer)

lazy val commonSettings = Seq(
  organization := "org.ukenergywatch",
  version := "0.0.1",
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature"
  ),
  libraryDependencies ++= Seq(
    "joda-time" % "joda-time" % "2.7",
    "org.joda" % "joda-convert" % "1.7",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "org.slf4j" % "slf4j-nop" % "1.6.4"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    "com.h2database" % "h2" % "1.4.187" % "test"
  )
)

lazy val utils = (project in file("utils"))
  .settings(commonSettings: _*)
  .settings(
    name := "utils"
  )

lazy val db = (project in file("db"))
  .settings(commonSettings: _*)
  .settings(
    name := "db"
  )
  .dependsOn(utils)

lazy val data = (project in file("data"))
  .settings(commonSettings: _*)
  .settings(
    name := "data"
  )
  .dependsOn(db)
  .dependsOn(utils)

lazy val importer = (project in file("importer"))
  .settings(commonSettings: _*)
  .settings(
    name := "importer",
    libraryDependencies ++= Seq(
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
    )
  )
  .dependsOn(db)
  .dependsOn(utils)
