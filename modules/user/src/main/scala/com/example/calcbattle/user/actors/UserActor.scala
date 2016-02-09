package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.{ShardRegion, ClusterShardingSettings, ClusterSharding}

object UserActor {
  def props = Props(new UserActor)
  val name = "UserActor"
  val nrOfShards = 50

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Subscribe(uid) => (uid.id, msg)
    case msg @ Result(uid, _) => (uid.id, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg @ Subscribe(uid) => (uid.hashCode % nrOfShards).toString
    case msg @ Result(uid, _) => (uid.hashCode % nrOfShards).toString
  }

  def startupSharding(system: ActorSystem) = {
    ClusterSharding(system).start(
      typeName = UserWorker.name,
      entityProps = UserWorker.props,
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }

  class UID(val id: String) extends AnyVal
  case class Subscribe(uid: UID)
  case class Result(uid: UID, isCorrect: Boolean)
}

class UserActor extends Actor {
  def receive = {
    case msg =>
      println("----------------------")
      println(msg)
      println("----------------------")
      val shardRegion = ClusterSharding(context.system).shardRegion(UserWorker.name)
      shardRegion forward msg
  }
}
