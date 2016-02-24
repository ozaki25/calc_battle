package com.example.calcbattle.user.actors

import akka.actor._
import akka.persistence.{RecoveryCompleted, PersistentActor}

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  case class UID(val id: String) extends AnyVal

  case class Join(uid: UID)
  case class UpdatedUserList(uids: Set[UID])

  sealed trait Event
  case class Joined(uid: UID) extends Event
  case class Left(uid: UID) extends Event
}

class FieldActor extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.FieldActor._

  override def persistenceId: String = name

  override def receiveRecover: Receive = {
    case event: Event =>
      log.info("receiveRecover")
      updateState(event)
    case RecoveryCompleted =>
      context.actorSelection("/system/sharding/UserWorker/*/*") ! UpdatedUserList(users)
  }

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Set[UID]()

  def receiveCommand = {
    case Join(uid) =>
      log.info("Join(uid) {}", uid)
      persist(Joined(uid))(updateState)
    case UserWorker.Stopped(uid) =>
      log.info("UserWorker.Stopped(uid) {}", uid)
      persist(Left(uid))(updateState)
    case SubscribeAck(Subscribe("join", None, self)) =>
      log.info("FieldActor subscribing 'join'")
  }

  def updateState(event: Event): Unit = {
    log.info(event.toString)
    event match {
      case Joined(uid) =>
        users += uid
        if(!recoveryRunning) context.actorSelection("/system/sharding/UserWorker/*/*") ! UpdatedUserList(users)
      case Left(uid) =>
        users -= uid
        if(!recoveryRunning) context.actorSelection("/system/sharding/UserWorker/*/*") ! UpdatedUserList(users)
    }
    log.info("user count {}", users.size.toString)
  }
}
