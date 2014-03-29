name := "root"

version := "0.1"

// This root project isn't really a play project, but this is required for the play command to work.
play.Project.playScalaSettings

lazy val utils = project

lazy val slogger = project
  .dependsOn(utils)

lazy val db = project
  .dependsOn(utils)

lazy val www = project
  .dependsOn(db)
  .dependsOn(utils)
  .dependsOn(slogger)

lazy val importer = project
  .dependsOn(utils)
  .dependsOn(db)
  .dependsOn(slogger)

lazy val root = project.in(file("."))