import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._

name := "root"

version := "0.1"

lazy val utils = project

lazy val slogger = project
  .dependsOn(utils)

lazy val db = project
  .dependsOn(utils)

lazy val wwwcommon = project

lazy val wwwjs = project
  .settings(
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "wwwcommon" / "src" / "main" / "scala"
  )

val scalajsOutputDir = Def.settingKey[File]("Directory for JS output from scala.js")

lazy val www = project
  .dependsOn(utils)
  .dependsOn(db)
  .dependsOn(slogger)
  .dependsOn(wwwcommon)
  .settings(
    scalajsOutputDir := (crossTarget in Compile).value,
    compile in Compile <<= (compile in Compile) dependsOn (preoptimizeJS in (wwwjs, Compile)),
    watchSources ++= ((sourceDirectory in (wwwjs, Compile)).value ** "*").get,
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  )
  .settings(
    Seq(packageExternalDepsJS, packageInternalDepsJS, packageExportedProductsJS, preoptimizeJS, optimizeJS) map { t =>
      crossTarget in (wwwjs, Compile, t) := scalajsOutputDir.value / "classes" / "js"
    }: _*
  )

lazy val importer = project
  .dependsOn(utils)
  .dependsOn(db)
  .dependsOn(slogger)

lazy val root = project.in(file("."))
