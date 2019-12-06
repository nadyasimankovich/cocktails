import sbt.Keys.{libraryDependencies, version}

name := "cocktails"

version := "1.0"

scalaVersion := "2.12.10"

val circeVersion = "0.11.1"
val finagleVersion = "19.11.0"
val gatlingVersion = "3.3.1"

scalacOptions += "-Ypartial-unification" // 2.11.9+

lazy val cocktails = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
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
  )
  .settings(
    packageName in Docker := "cocktails",
    version in Docker := "1.0.0",
    maintainer in Docker := "n.simankovich",
    dockerBaseImage in Docker := "anapsix/alpine-java:8_server-jre",
    dockerExposedPorts in Docker := Seq(8080)
  )

lazy val loadTest = Project("load", file("./load"))
  .enablePlugins(GatlingPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test,it",
      "io.gatling"            % "gatling-test-framework"    % gatlingVersion % "test,it"
    )
  )