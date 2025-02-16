ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "domain-modeling",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.1",
      "io.github.kitlangton" %% "neotype" % "0.3.15",
      "dev.zio" %% "zio-test" % "2.1.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
