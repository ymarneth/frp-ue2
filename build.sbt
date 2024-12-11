ThisBuild / version      := "1.0"
ThisBuild / scalaVersion := "3.6.1"
ThisBuild / scalacOptions := Seq("-unchecked", "-feature", "-deprecation")

val akkaVersion = "2.10.0"
val logbackVersion = "1.5.12"

lazy val root = (project in file("."))
  .settings(
    name := "frp-ue2",
    resolvers += "Akka Repository" at "https://repo.akka.io/maven/",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )
