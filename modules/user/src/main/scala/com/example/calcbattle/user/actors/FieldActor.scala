package com.example.calcbattle.user.actors

import akka.actor._
import akka.persistence.PersistentActor

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  case class UID(val id: String) extends AnyVal
  case class Join(uid: UID)
  case class Participation(uids: Set[UID])

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
    case event: Event => {
      log.info("receiveRecover")
      updateState(event)
    }
  }

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Set[UID]()

  def receiveCommand = {
    case Join(uid) =>
      log.info("Join(uid)")
      persist(Joined(uid))(updateState)
    case UserWorker.Stopped(uid) =>
      log.info("UserWorker.Stopped(uid)")
      persist(Left(uid))(updateState)
    case SubscribeAck(Subscribe("join", None, self)) =>
      log.info("FieldActor subscribing 'join'")
  }

  def updateState(event: Event): Unit = {
    log.info(event.toString)
    event match {
      case Joined(uid) =>
        users += uid
        context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users)
      case Left(uid) =>
        users -= uid
        context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users)
    }
    log.info("user count {}", users.size.toString)
  }
}
