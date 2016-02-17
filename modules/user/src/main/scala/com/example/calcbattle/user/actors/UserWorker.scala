package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"

  case class Result(uid: UID, isCorrect: Boolean)
  case class UpdateUser(uid: UID, continuationCorrect: Int) {
    val isFinish = continuationCorrect >= 5
  }
  case class Get(uid: UID)
  case class Stopped(uid: UID)
  case object DuplicateRequest
  case object Stop

  sealed trait Event
  case class Joined(socketActor: ActorRef, uid: UID) extends Event
  case class Answered(uid: UID, isCorrect: Boolean) extends Event
}
class UserWorker(field: ActorRef) extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.UserWorker._

  var continuationCorrect = 0
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
    case FieldActor.Join(uid) =>
      persist(Joined(sender, uid))(updateState)
  }
  def joined(socketActor: ActorRef, uid: UID): Receive = {
    case FieldActor.Join(uid) =>
      log.info("FieldActor.Join(uid)")
      context unwatch socketActor
      persist(Joined(sender, uid))(updateState)
    case Result(uid, isCorrect) =>
      log.info("Result(uid, isCorrect)")
      persist(Answered(uid, isCorrect))(updateState)
    case p:FieldActor.Participation =>
      log.info("p:FieldActor.Participation")
      socketActor ! p
    case u:UpdateUser =>
      log.info("u:UpdateUser")
      socketActor ! u
    case Get(uid) =>
      log.info("Get(uid)")
      sender ! UpdateUser(uid, continuationCorrect)
    case Terminated(user) =>
      log.info("Terminated(user)")
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
        continuationCorrect = 0
        context watch socketActor
        context.become(joined(socketActor, uid))
        mediator ! Publish("join", FieldActor.Join(uid))
      case Answered(uid, isCorrect) =>
        continuationCorrect = if(isCorrect) continuationCorrect + 1 else 0
        mediator ! Publish("update", UpdateUser(uid, continuationCorrect))
    }
  }
}
