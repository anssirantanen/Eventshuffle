name := """Eventshuffle"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  guice,

  "org.flywaydb"    %% "flyway-play"                  % "7.14.0",
  "org.postgresql"  % "postgresql"                    % "42.2.14",
  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
  "com.typesafe.play" %% "play-slick" % "5.0.0",

  "org.scalatestplus.play" %% "scalatestplus-play"    % "5.0.0" % Test
)

