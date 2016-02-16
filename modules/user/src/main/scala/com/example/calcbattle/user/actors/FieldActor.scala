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
  case class Joined(userWorker: ActorRef, uid: UID) extends Event
  case class Left(userWorker: ActorRef) extends Event
}

class FieldActor extends PersistentActor with ActorLogging {
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
  import com.example.calcbattle.user.actors.FieldActor._

  override def persistenceId: String = name

  override def receiveRecover: Receive = {
    case event: Event => updateState(event)
  }

  override def preStart() = {
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe("join", self)
  }

  var users = Map[ActorRef, UID]()

  def receiveCommand = {
    case Join(uid) =>
      persist(Joined(sender, uid))(updateState)
    case Terminated(user) =>
      persist(Left(user))(updateState)
    case SubscribeAck(Subscribe("join", None, self)) =>
      log.info("FieldActor subscribing 'join'")
  }

  def updateState(event: Event): Unit = {
    log.info(event.toString)
    event match {
      case Joined(userWorker, uid) =>
        users += (userWorker -> uid)
        context watch userWorker
        context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users.values.toSet)
      case Left(userWorker) =>
        users -= userWorker
        context unwatch userWorker
        context.actorSelection("/system/sharding/UserWorker/*/*") ! Participation(users.values.toSet)
    }
    log.info("user count {}", users.size.toString)
  }
}
