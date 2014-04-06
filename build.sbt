name := "root"

version := "0.1"

lazy val utils = project

lazy val slogger = project
  .dependsOn(utils)

lazy val db = project
  .dependsOn(utils)

lazy val www = project
  .dependsOn(utils)
  .dependsOn(slogger)

lazy val importer = project
  .dependsOn(utils)
  .dependsOn(db)
  .dependsOn(slogger)

lazy val root = project.in(file("."))
