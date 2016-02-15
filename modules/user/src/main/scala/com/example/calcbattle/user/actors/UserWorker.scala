package com.example.calcbattle.user.actors

import akka.actor.{ActorLogging, ActorRef, Props, Terminated}
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"

  case class Result(uid: UID, isCorrect: Boolean)
  case class UpdateUser(uid: UID, continuationCorrect: Int)
  case class Get(uid: UID)
  case object DuplicateRequest

  sealed trait Event
  // [TODO]

}
class UserWorker(field: ActorRef) extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.UserWorker._

  var socketActor: ActorRef = null
  var currentUid: UID = UID("0")
  var continuationCorrect = 0
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("update", self)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveRecover: Receive = {
    case event: Event =>
    // [TODO]
  }

  override def receiveCommand = {
    case FieldActor.Join(uid) =>
      if(currentUid == uid) {
        sender ! DuplicateRequest
      } else {
        socketActor = sender
        currentUid = uid
        context watch socketActor
        mediator ! Publish("join", FieldActor.Join(uid))
      }
    case Result(uid, isCorrect) =>
      continuationCorrect = if(isCorrect) continuationCorrect + 1 else 0
      mediator forward Publish("update", UpdateUser(uid, continuationCorrect))
    case p:FieldActor.Participation =>
      socketActor ! p
    case u:UpdateUser =>
      socketActor ! u
    case Get(uid) =>
      sender ! UpdateUser(uid, continuationCorrect)
    case Terminated(user) =>
      context.stop(self)
    case SubscribeAck(Subscribe("update", None, self)) =>
      log.info("UserWorker subscribing 'update'")
  }

  def updateState(event: Event): Unit = event match {
    case _ =>
    // [TODO]
  }
}
