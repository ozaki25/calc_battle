package com.example.calcbattle.user

import akka.actor.{ActorPath, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import com.example.calcbattle.user.actors.UserActor
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  args match {
    case Array(hostname, port) =>
      val config =
        ConfigFactory.parseString(
          s"""
           |akka.remote.netty.tcp.hostname = ${args(0)}
           |akka.remote.netty.tcp.port     = ${args(1)}
           |""".stripMargin
        ).withFallback(ConfigFactory.load())

      val system = ActorSystem("application", config)
      /*UserActor.startupSharedJournal(
        system, startStore = (port == "2551"),
        path = ActorPath.fromString("akka.tcp://application@127.0.0.1:2551/user/store")
      )*/
      UserActor.startupSharding(system)
      system.actorOf(UserActor.props, UserActor.name)
      Await.result(system.whenTerminated, Duration.Inf)
    case _ =>
      throw new IllegalArgumentException("引数には <ホスト名> <ポート番号> を指定してください。")
  }
}
