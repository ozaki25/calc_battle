val namePrefix = "calc_battle"

lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-frontend""",
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" % "akka-cluster-metrics_2.11" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator
  )
