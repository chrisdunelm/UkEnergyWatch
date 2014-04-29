scalaJSSettings

name := "wwwjs"

version := "0.1"

libraryDependencies ++= Seq(
  "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3",
  "org.scalajs" %% "scalajs-pickling" % "0.2",
  "com.scalatags" %% "scalatags" % "0.2.5-JS"
)
