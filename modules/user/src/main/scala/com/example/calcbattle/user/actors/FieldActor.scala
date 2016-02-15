package com.example.calcbattle.user.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.example.calcbattle.user.actors.FieldActor._

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  case class UID(val id: String) extends AnyVal
  case class Join(uid: UID)
  case class Participation(userWorkers: Set[UID])
}

class FieldActor extends Actor {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Map[ActorRef, UID]()

  def receive = {
    case Join(uid) =>
      println("------fieldActor_join------")
      println(users)
      users += (sender -> uid)
      println(users)
      context watch sender
      sender ! Participation(users.valuesIterator.toSet)
      println("----------------------")
    case Terminated(user) =>
      println("------fieldActor_terminated------")
      println(users)
      users -= user
      println(users)
      println("----------------------")
    case SubscribeAck(Subscribe("join", None, `self`)) =>
      println("----FieldActor subscribing Join----")
  }
}
