package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.PersistentActor

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"
}

class UserWorker(field: ActorRef) extends PersistentActor {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

  override def preStart() = println("----------UserWorker_Start-------------")

  override def postStop() = println("----------UserWorker_Stop--------------")

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("update", self)

  var socketActor: ActorRef = null

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
  override def receiveRecover: Receive = {
    case _ =>
      println("receiveRecover")
  }
  override def receiveCommand = {
    case FieldActor.Join(uid) =>
      println("------userWorker_join------")
      socketActor = sender
      context watch socketActor
      mediator ! Publish("join", FieldActor.Join(uid))
      println("----------------------")
    case p:FieldActor.Participation =>
      println("------userWorker_participation------")
      socketActor ! p
      println("----------------------")
    case Terminated(user) =>
      println("------userWorker_terminated------")
      context.stop(self)
      println("----------------------")
    case msg =>
      println("------userWorker_msg------")
      println(msg)
      println("----------------------")
  }
}
