package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"

  case class UpdateName(uid: UID, name: String)
  case class UpdateCorrectCount(uid: UID, isCorrect: Boolean)
  case class Create(uid: UID)
  case class Get(uid: UID)
  case object Stop

  case class Updated(uid: UID, correctCount: Int, name: String) {
    val isFinish = correctCount >= 5
  }
  case class Stopped(uid: UID)
  case object DuplicateRequest

  sealed trait Event
  case class Joined(socketActor: ActorRef, uid: UID) extends Event
  case class Registered(uid: UID, name: String) extends Event
  case class Answered(uid: UID, isCorrect: Boolean) extends Event
}

class UserWorker(field: ActorRef) extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.UserWorker._

  var nicName = ""
  var correctCount = 0
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("update", self)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveRecover: Receive = {
    case event: Event => {
      log.info("receiveRecover")
      updateState(event)
    }
  }

  override def receiveCommand = initial

  def initial: Receive = {
    case Create(uid) =>
      persist(Joined(sender, uid))(updateState)
  }
  def joined(socketActor: ActorRef, uid: UID): Receive = {
    case Create(uid) =>
      log.info("FieldActor.Join(uid) {}", uid)
      context unwatch socketActor
      persist(Joined(sender, uid))(updateState)
    case UpdateName(uid, name) =>
      log.info("UpdateName(uid, isCorrect) {} {}", uid, name)
      persist(Registered(uid, name))(updateState)
    case UpdateCorrectCount(uid, isCorrect) =>
      log.info("UpdateCorrectCount(uid, isCorrect) {} {}", uid, isCorrect)
      persist(Answered(uid, isCorrect))(updateState)
    case u:FieldActor.UpdatedUserList =>
      log.info("p:FieldActor.Participation {}", u)
      socketActor ! u
    case u:Updated =>
      log.info("u:UpdateUser {}", u)
      socketActor ! u
    case Get(uid) =>
      log.info("Get(uid) {}", uid)
      sender ! Updated(uid, correctCount, nicName)
    case Terminated(user) =>
      log.info("Terminated(user) {}", user)
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info("Stop")
      mediator ! Publish("join", Stopped(uid))
      context.stop(self)
    case SubscribeAck(Subscribe("update", None, self)) =>
      log.info("UserWorker subscribing 'update'")
  }

  def updateState(event: Event): Unit = {
    log.info(event.toString)
    event match {
      case Joined(socketActor, uid) =>
        nicName = uid.id.toString
        correctCount = 0
        context watch socketActor
        context.become(joined(socketActor, uid))
        mediator ! Publish("join", FieldActor.Join(uid))
      case Registered(uid, name) =>
        nicName = name
        mediator ! Publish("update", Updated(uid, correctCount, nicName))
      case Answered(uid, isCorrect) =>
        correctCount = if(isCorrect) correctCount + 1 else 0
        mediator ! Publish("update", Updated(uid, correctCount, nicName))
    }
  }
}
