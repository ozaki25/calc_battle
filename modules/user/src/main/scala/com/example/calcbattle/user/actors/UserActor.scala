package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.{ShardRegion, ClusterShardingSettings, ClusterSharding}

object UserActor {
  def props = Props(new UserActor)
  val name = "UserActor"
  val nrOfShards = 50

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ UserWorker.Create(uid) => (uid.id, msg)
    case msg @ UserWorker.UpdateName(uid, _) => (uid.id, msg)
    case msg @ UserWorker.UpdateCorrectCount(uid, _) => (uid.id, msg)
    case msg @ UserWorker.Get(uid) => (uid.id, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg @ UserWorker.Create(uid) => (uid.hashCode % nrOfShards).toString
    case msg @ UserWorker.UpdateName(uid, _) => (uid.hashCode % nrOfShards).toString
    case msg @ UserWorker.UpdateCorrectCount(uid, _) => (uid.hashCode % nrOfShards).toString
    case msg @ UserWorker.Get(uid) => (uid.hashCode % nrOfShards).toString
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

class UserActor extends Actor with ActorLogging {
  def receive = {
    case msg =>
      log.info("msg {}", msg)
      val shardRegion = ClusterSharding(context.system).shardRegion(UserWorker.name)
      shardRegion forward msg
  }
}
