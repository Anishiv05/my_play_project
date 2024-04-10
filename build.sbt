name := """my_play_project"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.13"

libraryDependencies += "com.typesafe.play" %% "play-guice" % "2.8.10"

libraryDependencies += "com.stripe" % "stripe-java" % "20.80.0"
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "8.0.1"
libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0"




libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "com.typesafe.play" %% "play-slick"            % "5.3.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.3.0",
  "com.h2database" % "h2" % "1.4.200",
  "org.postgresql" % "postgresql" % "42.2.24" // PostgreSQL dependency

)