
lazy val root = (project in file("."))
  .aggregate(utils)
  .aggregate(db)
  .aggregate(data)
  .aggregate(importer)

lazy val commonSettings = Seq(
  organization := "org.ukenergywatch",
  version := "0.0.1",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature"
  ),
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies ++= Seq(
    //"joda-time" % "joda-time" % "2.7",
    //"org.joda" % "joda-convert" % "1.7",
    //"codes.reactive" %% "scala-time" % "0.3.0-SNAPSHOT",
    "com.typesafe.slick" %% "slick" % "3.1.0",
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
    name := "utils",
    libraryDependencies ++= Seq(
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
    )
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
      "com.github.scopt" %% "scopt" % "3.3.0"
    )
  )
  .dependsOn(db)
  .dependsOn(utils)

lazy val oldfueltype = (project in file("oldfueltype"))
  .settings(commonSettings: _*)
  .settings(
    name := "oldfueltype",
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % "5.1.37"
    )
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(utils)
