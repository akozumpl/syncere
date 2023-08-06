Global / onChangedBuildSource := ReloadOnSourceChanges

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
      "software.amazon.awssdk" % "s3" % "2.20.116",
      // Scala deps, working in native
      "org.typelevel" %% "cats-core" % "2.9.0",
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
