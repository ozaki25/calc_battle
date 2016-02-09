val namePrefix = "calc_battle"

lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.7"
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
      "com.typesafe.akka" %% "akka-cluster-metrics" % "2.4.1",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
      specs2 % Test
    ),

    // Play provides two styles of routers, one expects its actions to be injected, the
    // other, legacy style, accesses its actions statically.
    routesGenerator := InjectedRoutesGenerator
  ).dependsOn(examiner, user)

lazy val examiner = (project in file("modules/examiner"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-examiner""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.examiner.Main", "127.0.0.1", "2552")
  )

lazy val user = (project in file("modules/user"))
  .settings(commonSettings: _*)
  .settings(
    name := s"""$namePrefix-user""",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
      "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.1",
      "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
      "org.iq80.leveldb"  %  "leveldb" % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
    ),
    fullRunInputTask(run, Compile, "com.example.calcbattle.user.Main", "127.0.0.1", "0"),
    fullRunTask(runSeed, Compile, "com.example.calcbattle.user.Main", "127.0.0.1", "2553")
  )

lazy val runSeed = TaskKey[Unit]("run-seed", "run one node as seed.")
