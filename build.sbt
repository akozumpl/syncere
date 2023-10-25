Global / onChangedBuildSource := ReloadOnSourceChanges

val CirceVersion = "0.14.5"
val Slf4jVersion = "2.0.9"

lazy val root = (project in file("."))
  .settings(
    name := "syncere",
    organization := "org.cative",
    version := "0.1-SNAPSHOT",
    scalaVersion := "3.3.0",
    scalacOptions ++= Seq(
      "-deprecation",
      "-explaintypes",
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps"
    ),
    libraryDependencies ++= Seq(
      // Java deps (no native equivalent)
      "org.slf4j" % "slf4j-simple" % Slf4jVersion,
      "software.amazon.awssdk" % "s3" % "2.20.116",
      // Scala deps, working in native
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %% "cats-effect" % "3.5.1",
      // test
      "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    ),
    cancelable in Global := true,
    fork := true,
    addCommandAlias(
      "prep",
      List("scalafixAll", "scalafmtSbt", "scalafmtAll", "test").mkString(";")
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
