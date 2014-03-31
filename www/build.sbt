name := "www"

version := "0.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.scalatags" % "scalatags_2.10" % "0.2.4"
)     

play.Project.playScalaSettings
