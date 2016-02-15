package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.{ShardRegion, ClusterSharding, ClusterShardingSettings}
import akka.persistence.PersistentActor

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"
  val nrOfShards = 50

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Join(uid) => (uid.id, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case msg @ Join(uid) => (uid.hashCode % nrOfShards).toString
  }

  def startupSharding(system: ActorSystem) = {
    ClusterSharding(system).start(
      typeName = FieldActor.name,
      entityProps = FieldActor.props,
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }

  case class UID(val id: String) extends AnyVal
  case class Join(uid: UID)
  case class Participation(uids: Set[UID])

  sealed trait Event
  // [TODO]
}

class FieldActor extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.FieldActor._

  override def persistenceId: String = name

  override def receiveRecover: Receive = {
    case event: Event =>
    // [TODO]
  }

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Map[ActorRef, UID]()

  def receiveCommand = {
    case Join(uid) =>
      users += (sender -> uid)
      context watch sender
      context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users.values.toSet)
    case Terminated(user) =>
      users -= user
      context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users.values.toSet)
    case SubscribeAck(Subscribe("join", None, self)) =>
      log.info("FieldActor subscribing 'join'")
  }
}
