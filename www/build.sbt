name := "www"

version := "0.1"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.slf4j" % "slf4j-api" % "1.7.6",
  "org.scalatra" %% "scalatra" % "2.2.2",
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.3.v20140225",
  "com.scalatags" % "scalatags_2.10" % "0.2.4",
  "org.scalajs" %% "scalajs-pickling-play-json" % "0.2",
  "mysql" % "mysql-connector-java" % "5.1.29"
)

Revolver.settings