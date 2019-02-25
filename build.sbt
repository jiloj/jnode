import sbt.Keys._

scalaVersion in ThisBuild := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")

def gatlingVersion(scalaBinVer: String): String = scalaBinVer match {
  case "2.11" => "2.2.5"
  case "2.12" => "2.3.1"
}

libraryDependencies += guice
libraryDependencies += "org.joda" % "joda-convert" % "1.9.2"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.11"

libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.1"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.47"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.1.0"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "1.0-M1"

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .settings(
    name := """jnode"""
  )
