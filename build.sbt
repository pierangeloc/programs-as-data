ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.tuliplogicbv"

ThisBuild / scalacOptions ++= Seq()

val doobieV = "1.0.0-RC7"
val slickV  = "3.5.2"

lazy val healthCheck = project
  .in(file("health-check"))
  .settings(
    name := "health-check",
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % "2.1.15",
      "io.github.kitlangton"          %% "neotype"                       % "0.3.15",
      "io.github.kitlangton"          %% "neotype-doobie"                % "0.3.15",
      "org.tpolecat"                  %% "doobie-core"                   % doobieV,
      "org.tpolecat"                  %% "doobie-hikari"                 % doobieV,
      "org.tpolecat"                  %% "doobie-postgres"               % doobieV,
      "org.tpolecat"                  %% "doobie-mysql"                  % doobieV,
      "dev.zio"                       %% "zio-interop-cats"              % "23.1.0.4",
      "mysql"                          % "mysql-connector-java"          % "8.0.33",
      "dev.zio"                       %% "zio-test"                      % "2.1.15" % Test,
      "dev.zio"                       %% "zio-kafka"                     % "2.11.0",
      "dev.zio"                       %% "zio-logging-slf4j"             % "2.5.0",
      "ch.qos.logback"                 % "logback-classic"               % "1.5.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.10.3",
      "com.typesafe.slick"            %% "slick"                         % slickV,
      "com.typesafe.slick"            %% "slick-hikaricp"                % slickV
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(infraUtils)

lazy val ruleEngine = project
  .in(file("rule-engine"))
  .settings(
    name := "rule-engine",
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % "2.1.15",
      "io.github.kitlangton"          %% "neotype"                       % "0.3.15",
      "io.github.kitlangton"          %% "neotype-doobie"                % "0.3.15",
      "org.tpolecat"                  %% "doobie-core"                   % doobieV,
      "org.tpolecat"                  %% "doobie-hikari"                 % doobieV,
      "org.tpolecat"                  %% "doobie-postgres"               % doobieV,
      "org.tpolecat"                  %% "doobie-mysql"                  % doobieV,
      "dev.zio"                       %% "zio-interop-cats"              % "23.1.0.4",
      "mysql"                          % "mysql-connector-java"          % "8.0.33",
      "dev.zio"                       %% "zio-test"                      % "2.1.15" % Test,
      "dev.zio"                       %% "zio-kafka"                     % "2.11.0",
      "dev.zio"                       %% "zio-logging-slf4j"             % "2.5.0",
      "ch.qos.logback"                 % "logback-classic"               % "1.5.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.10.3"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(dataStructures)
  .dependsOn(infraUtils)

lazy val infraUtils = project
  .in(file("infra-utils"))
  .settings(
    name := "infra-utils",
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % "2.1.15",
      "io.github.kitlangton"          %% "neotype"                       % "0.3.15",
      "io.github.kitlangton"          %% "neotype-doobie"                % "0.3.15",
      "org.tpolecat"                  %% "doobie-core"                   % doobieV,
      "org.tpolecat"                  %% "doobie-hikari"                 % doobieV,
      "org.tpolecat"                  %% "doobie-postgres"               % doobieV,
      "org.tpolecat"                  %% "doobie-mysql"                  % doobieV,
      "dev.zio"                       %% "zio-interop-cats"              % "23.1.0.4",
      "mysql"                          % "mysql-connector-java"          % "8.0.33",
      "dev.zio"                       %% "zio-test"                      % "2.1.15" % Test,
      "dev.zio"                       %% "zio-kafka"                     % "2.11.0",
      "dev.zio"                       %% "zio-logging-slf4j"             % "2.5.0",
      "ch.qos.logback"                 % "logback-classic"               % "1.5.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.10.3"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(dataStructures)

lazy val dataStructures = project
  .in(file("data-structures"))
  .settings(
    name := "data-structures",
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % "2.1.15",
      "io.github.kitlangton"          %% "neotype"                       % "0.3.15",
      "io.github.kitlangton"          %% "neotype-doobie"                % "0.3.15",
      "org.tpolecat"                  %% "doobie-core"                   % doobieV,
      "org.tpolecat"                  %% "doobie-hikari"                 % doobieV,
      "org.tpolecat"                  %% "doobie-postgres"               % doobieV,
      "org.tpolecat"                  %% "doobie-mysql"                  % doobieV,
      "dev.zio"                       %% "zio-interop-cats"              % "23.1.0.4",
      "mysql"                          % "mysql-connector-java"          % "8.0.33",
      "dev.zio"                       %% "zio-test"                      % "2.1.15" % Test,
      "dev.zio"                       %% "zio-kafka"                     % "2.11.0",
      "dev.zio"                       %% "zio-logging-slf4j"             % "2.5.0",
      "ch.qos.logback"                 % "logback-classic"               % "1.5.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.10.3"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val root = (project in file("."))
  .aggregate(healthCheck)
  .aggregate(ruleEngine)

/*
lazy val root = (project in file("."))
  .settings(
    name := "domain-modeling",
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % "2.1.15",
      "io.github.kitlangton"          %% "neotype"                       % "0.3.15",
      "io.github.kitlangton"          %% "neotype-doobie"                % "0.3.15",
      "org.tpolecat"                  %% "doobie-core"                   % doobieV,
      "org.tpolecat"                  %% "doobie-hikari"                 % doobieV,
      "org.tpolecat"                  %% "doobie-postgres"               % doobieV,
      "org.tpolecat"                  %% "doobie-mysql"                  % doobieV,
      "dev.zio"                       %% "zio-interop-cats"              % "23.1.0.4",
      "mysql"                          % "mysql-connector-java"          % "8.0.33",
      "dev.zio"                       %% "zio-test"                      % "2.1.15" % Test,
      "dev.zio"                       %% "zio-kafka"                     % "2.11.0",
      "dev.zio"                       %% "zio-logging-slf4j"             % "2.5.0",
      "ch.qos.logback"                 % "logback-classic"               % "1.5.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.10.3"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
 */
