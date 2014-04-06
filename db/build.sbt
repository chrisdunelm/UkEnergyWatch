
name := "db"

version := "0.1"

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "com.typesafe.slick" %% "slick" % "2.0.1",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.h2database" % "h2" % "1.3.175" % "test"
)