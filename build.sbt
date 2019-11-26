

name := "cocktails"

version := "1.0"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"
val finagleVersion = "19.11.0"
//val doobieVersion = "0.8.4"

//resolvers += "Couchbase Snapshots" at "http://files.couchbase.com/maven2"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.2",

  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finatra-http" % finagleVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "com.datastax.cassandra" % "cassandra-driver-core" % "3.8.0"
  //  "com.couchbase.client" %% "scala-client" % "1.0.0-beta.1"

//  "org.tpolecat" %% "doobie-core"      % doobieVersion,
//
//  // And add any of these as needed
//  "org.tpolecat" %% "doobie-h2"        % doobieVersion,          // H2 driver 1.4.199 + type mappings.
//  "org.tpolecat" %% "doobie-hikari"    % doobieVersion,          // HikariCP transactor.
//  "org.tpolecat" %% "doobie-postgres"  % doobieVersion,          // Postgres driver 42.2.8 + type mappings.
//  "org.tpolecat" %% "doobie-quill"     % doobieVersion,          // Support for Quill 3.4.9
//  "org.tpolecat" %% "doobie-specs2"    % doobieVersion % "test", // Specs2 support for typechecking statements.
//  "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "test"  // ScalaTest support for typechecking statements.
)

scalacOptions += "-Ypartial-unification" // 2.11.9+
