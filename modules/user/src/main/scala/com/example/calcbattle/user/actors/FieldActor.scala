package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, ActorLogging, Actor, Props, Terminated}

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  case class UID(val id: String) extends AnyVal
  case class Join(uid: UID)
  case class Participation(uids: Set[UID])
}

class FieldActor extends Actor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.FieldActor._

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Map[ActorRef, UID]()

  def receive = {
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
