
lazy val root = (project in file("."))
  .aggregate(utils)
  .aggregate(db)
  .aggregate(data)
  .aggregate(importers)
  .aggregate(initmysql)
  .aggregate(appimporter)
  .aggregate(oldfueltype)

lazy val baseSettings = Seq(
  organization := "org.ukenergywatch",
  version := "0.0.1",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature"
  ),
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

lazy val commonSettings = baseSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "3.1.0",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "com.h2database" % "h2" % "1.4.187"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  )
)

lazy val utils = (project in file("utils"))
  .settings(commonSettings: _*)
  .settings(
    name := "utils",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.7",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
    )
  )

lazy val db = (project in file("db"))
  .settings(commonSettings: _*)
  .settings(
    name := "db",
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % "5.1.37"
    )
  )
  .dependsOn(utils)

lazy val data = (project in file("data"))
  .settings(commonSettings: _*)
  .settings(
    name := "data"
  )
  .dependsOn(db)
  .dependsOn(utils)

import ScalaxbKeys._
lazy val importers = (project in file("importers"))
  .settings(commonSettings: _*)
  .settings(
    name := "importers",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
    )
  )
  .settings(scalaxbSettings: _*)
  .settings(
    sourceGenerators in Compile += (scalaxb in Compile).taskValue,
    dispatchVersion in (Compile, scalaxb) := "0.11.2",
    ScalaxbKeys.packageName in (Compile, scalaxb) := "InstantaneousFlowWebService",
    async in (Compile, scalaxb) := true
    //logLevel in (Compile, scalaxb) := Level.Debug
  )
  .dependsOn(data)
  .dependsOn(db)
  .dependsOn(utils)

lazy val initmysql = (project in file("initmysql"))
  .settings(commonSettings: _*)
  .settings(
    name := "initmysql"
  )
  .dependsOn(db)
  .dependsOn(utils)

// Use universal:packageZipTarball from sbt to package into .tgz
lazy val appimporter = (project in file("appimporter"))
  .settings(commonSettings: _*)
  .settings(
    name := "appimporter"
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(importers)
  .dependsOn(data)
  .dependsOn(db)
  .dependsOn(utils)

// Use universal:packageZipTarball from sbt to package into .tgz
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

lazy val www = crossProject.in(file("www"))
  .settings(baseSettings: _*)
  .settings(
    name := "www",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.5.3",
      "me.chrons" %%% "boopickle" % "1.1.1"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scalatra" %% "scalatra" % "2.4.0",
      "org.eclipse.jetty" % "jetty-webapp" % "9.2.14.v20151106",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"
    ),
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test"
    )
  )
  .jvmConfigure(_
    .dependsOn(db)
    .dependsOn(utils)
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.2"
    ),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.3.1" % "test"
    )
  )
lazy val wwwJS = www.js
lazy val wwwJVM = www.jvm.settings(
  // Copy generated JS file to www server
  resourceGenerators in Compile <+=
    (resourceManaged in Compile, fastOptJS in (wwwJS, Compile)) map { (dir, jsIn) =>
      // TODO: Copy sourcemap
      val jsInFile = jsIn.data
      val jsOutFile = dir / "js" / jsInFile.getName
      IO.copyFile(jsInFile, jsOutFile)
      Seq(jsOutFile)
    }
)
