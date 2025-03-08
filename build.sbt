ThisBuild / scalaVersion     := "3.3.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val doobieV = "1.0.0-RC8"

lazy val root = (project in file("."))
  .settings(
    name := "domain-modeling",
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio"                  % "2.1.1",
      "io.github.kitlangton" %% "neotype"              % "0.3.15",
      "org.tpolecat"         %% "doobie-core"          % doobieV,
      "org.tpolecat"         %% "doobie-hikari"        % doobieV,
      "org.tpolecat"         %% "doobie-postgres"      % doobieV,
      "org.tpolecat"         %% "doobie-mysql"         % doobieV,
      "dev.zio"              %% "zio-interop-cats"     % "23.1.0.4",
      "mysql"                 % "mysql-connector-java" % "8.0.33",
      "dev.zio"              %% "zio-test"             % "2.1.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
