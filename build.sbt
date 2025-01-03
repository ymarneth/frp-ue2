ThisBuild / version      := "1.0"
ThisBuild / scalaVersion := "3.6.1"
ThisBuild / scalacOptions := Seq("-unchecked", "-feature", "-deprecation")

val akkaVersion = "2.10.0"
val logbackVersion = "1.5.15"
val circeVersion   = "0.14.10"

lazy val root = (project in file("."))
  .settings(
    name := "frp-ue2",
    resolvers += "Akka Repository" at "https://repo.akka.io/maven/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,

      "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "9.0.0",
      "com.typesafe.slick" %% "slick" % "3.5.2",
      "com.h2database" % "h2" % "2.3.232",

      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )
