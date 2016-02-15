package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"

  case class Result(uid: UID, isCorrect: Boolean)
  case class UpdateUser(uid: UID, continuationCorrect: Int)
  case class Get(uid: UID)

  sealed trait Event
  case class Joined(uid: UID, socketActor: ActorRef) extends Event

}
class UserWorker(field: ActorRef) extends PersistentActor {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.UserWorker._

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("update", self)

  var socketActor: ActorRef = null
  var continuationCorrect = 0

  override def preStart() = println("----------UserWorker_Start-------------")
  override def postStop() = println("----------UserWorker_Stop--------------")

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveRecover: Receive = {
    case event: Event =>
      println("-----receiveRecover--------")
      updateState(event)
      println("---------------------------")
  }

  override def receiveCommand = {
    case FieldActor.Join(uid) =>
      println("------userWorker_join------")
      socketActor = sender
      context watch socketActor
      mediator ! Publish("join", FieldActor.Join(uid))
      persist(Joined(uid, socketActor))(updateState)
      println("----------------------")
    case p:FieldActor.Participation =>
      println("------userWorker_participation------")
      socketActor ! p
      println("----------------------")
    case Result(uid, isCorrect) =>
      println("------userWorker_result------")
      continuationCorrect = if(isCorrect) continuationCorrect + 1 else 0
      mediator forward Publish("update", UpdateUser(uid, continuationCorrect))
      println("----------------------")
    case u:UpdateUser =>
      println("------userWorker_updateUser------")
      socketActor ! u
      println("----------------------")
    case Get(uid) =>
      sender ! UpdateUser(uid, continuationCorrect)
    case Terminated(user) =>
      println("------userWorker_terminated------")
      context.stop(self)
      println("----------------------")
    case SubscribeAck(Subscribe("update", None, `self`)) =>
      println("----UserWorker subscribing Update----")
    case msg =>
      println("------userWorker_msg------")
      println(msg)
      println("----------------------")
  }

  def updateState(event: Event): Unit = event match {
    case Joined(uid, actor) =>
      println("----updateState(Joined)---")
      println(socketActor)
      socketActor = actor
  }
}
