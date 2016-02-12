package com.example.calcbattle.user.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ShardRegion, ClusterShardingSettings, ClusterSharding}
import com.example.calcbattle.user.actors.FieldActor.Join

object UserActor {
  def props = Props(new UserActor)
  val name = "UserActor"
  val nrOfShards = 50

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Join(uid) => (uid.id, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg @ Join(uid) => (uid.hashCode % nrOfShards).toString
  }

  def startupSharding(system: ActorSystem, field :ActorRef) = {
    ClusterSharding(system).start(
      typeName = UserWorker.name,
      entityProps = UserWorker.props(field),
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }
}

class UserActor extends Actor {
  def receive = {
    case msg =>
      println("------userActor------")
      val shardRegion = ClusterSharding(context.system).shardRegion(UserWorker.name)
      shardRegion forward msg
      println("---------------------")
  }
}
