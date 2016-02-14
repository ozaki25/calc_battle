package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"

  case class Result(isCorrect: Boolean)
  case class User(uid: UID, continuationCorrect: Int)
}
class UserWorker(field: ActorRef) extends PersistentActor {
  import com.example.calcbattle.user.actors.FieldActor.Join
  import com.example.calcbattle.user.actors.UserWorker.{Result, User}
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("update", self)

  var socketActor: ActorRef = null
  var continuationCorrect = 0

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
  override def receiveRecover: Receive = {
    case _ =>
      println("receiveRecover")
  }
  override def receiveCommand = {
    case Join(uid) =>
      println("------userWorker_join------")
      socketActor = sender()
      mediator forward Publish("join", Join(uid))
      println("----------------------")
    case Result(isCorrect) =>
      println("------userWorker_result------")
      continuationCorrect = if(isCorrect) continuationCorrect + 1 else 0
      mediator forward Publish("update", continuationCorrect)
      println("----------------------")
    case u:User =>
      println("------userWorker_user------")
      socketActor ! u
      println("----------------------")
    case msg =>
      println("------userWorker_msg------")
      println(msg)
      println("----------------------")
  }
}
