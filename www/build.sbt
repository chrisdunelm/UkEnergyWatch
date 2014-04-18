name := "www"

version := "0.1"

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.2.2",
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.3.v20140225",
  "com.scalatags" % "scalatags_2.10" % "0.2.4",
  "org.scalajs" %% "scalajs-pickling-play-json" % "0.2"
)

Revolver.settings