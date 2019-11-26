name := "cocktails"

version := "1.0"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"
val finagleVersion = "19.11.0"

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.2",

  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finatra-http" % finagleVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "com.datastax.cassandra" % "cassandra-driver-core" % "3.8.0",
  "com.github.blemale" %% "scaffeine" % "3.1.0" % "compile"
)

scalacOptions += "-Ypartial-unification" // 2.11.9+

packageName in Docker := "cocktails"

version in Docker := "1.0.0"

maintainer in Docker := "n.simankovich"

dockerBaseImage in Docker := "anapsix/alpine-java:8_server-jre"

dockerExposedPorts in Docker := Seq(8080)