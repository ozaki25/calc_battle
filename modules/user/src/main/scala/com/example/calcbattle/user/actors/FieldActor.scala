package com.example.calcbattle.user.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.example.calcbattle.user.actors.FieldActor.UID

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  class UID(val id: String) extends AnyVal
  case class Join(uid: UID)
  case class Participation(users: Set[UID])
}

class FieldActor extends Actor {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import FieldActor.{Join, Participation}

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("userJoin", self)
  }

  var users = Map[ActorRef, UID]()

  def receive = {
    case Join(uid) =>
      println("------fieldActor_join------")
      println(users)
      users += (sender -> uid)
      println(users)
      context watch sender
      sender() ! Participation(users.values.toSet)
      println("----------------------")
    case Terminated(user) =>
      println("------fieldActor_terminated------")
      println(users)
      users -= user
      println(users)
      println("----------------------")
    case SubscribeAck(Subscribe("userJoin", None, `self`)) =>
      println("subscribing")
  }
}
