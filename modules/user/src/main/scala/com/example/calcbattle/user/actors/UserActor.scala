package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.{ShardRegion, ClusterShardingSettings, ClusterSharding}
import akka.persistence.journal.leveldb.{SharedLeveldbStore, SharedLeveldbJournal}
import akka.util.Timeout
import com.example.calcbattle.user.actors.UserActor._
import scala.concurrent.duration._

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
      typeName = UserActor.name,
      entityProps = UserActor.props,
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }
/*
  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    if(startStore) system.actorOf(Props[SharedLeveldbStore], "store")
    implicit val timeout = Timeout(15.seconds)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }
*/
  class UID(val id: String) extends AnyVal
  case class Subscribe(uid: UID)
  case class Result(uid: UID, isCorrect: Boolean)
}

class UserActor extends Actor {
  def receive = {
    case msg =>
      val shardRegion = ClusterSharding(context.system).shardRegion(name)
      println(msg)
      //shardRegion forward msg
    /*
    case Subscribe(uid) =>
      println("----------------------")
      println(uid)
      println("----------------------")
    case Result(uid, isCorrect) =>
      println("----------------------")
      println(uid)
      println(isCorrect)
      println("----------------------")
      */
  }
}
