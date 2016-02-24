package com.example.calcbattle.user.actors

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Unsubscribe
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.UID

import scala.concurrent.Future

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
  case class AlreadyStopped(msg: String)
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
  lazy val piWeight = context.system.settings.config.getLong("calc_battle.pi-weight")

  override def preStart() = mediator ! Subscribe("update", self)

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
      log.info("def: initial, case: Create(uid) {}", uid)
      persist(Joined(sender, uid))(updateState)
  }

  def joined(socketActor: ActorRef, uid: UID): Receive = {
    case Create(uid) =>
      log.info("def: joined, case: FieldActor.Create(uid) {}", uid)
      context unwatch socketActor
      persist(Joined(sender, uid))(updateState)
    case UpdateName(uid, name) =>
      log.info("def: joined, case: UpdateName(uid, isCorrect) {} {}", uid, name)
      persist(Registered(uid, name))(updateState)
    case UpdateCorrectCount(uid, isCorrect) =>
      log.info("def: joined, case: UpdateCorrectCount(uid, isCorrect) {} {}", uid, isCorrect)
      persist(Answered(uid, isCorrect))(updateState)
    case u:FieldActor.UpdatedUserList =>
      log.info("def: joined, case: u:FieldActor.UpdatedUserList {}", u)
      socketActor ! u
    case u:Updated =>
      log.info("def: joined, case: u:UpdateUser {}", u)
      socketActor ! u
    case Get(uid) =>
      log.info("def: joined, case: Get(uid) {}", uid)
      sender ! Updated(uid, correctCount, nicName)
    case Terminated(user) =>
      log.info("def: joined, case: Terminated(user) {}", user)
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info("def: joined, case: Stop")
      context.become(stopped())
      mediator ! Unsubscribe("update", self)
      mediator ! Publish("join", Stopped(uid))
      context.stop(self)
    case SubscribeAck(Subscribe("update", None, self)) =>
      log.info("UserWorker subscribing 'update'")
  }

  def stopped(): Receive = {
    case Get(uid) =>
      log.info("def: stopped, case: Get(uid) {}", uid)
      log.info("already stopped")
      sender ! AlreadyStopped("already stopped")
      context.parent ! Passivate(stopMessage = Stop)
    case Stop =>
      log.info("def: stopped, case: Stop")
      context.stop(self)
  }

  def updateState(event: Event): Unit = {
    log.info(event.toString)
    event match {
      case Joined(socketActor, uid) =>
        nicName = uid.id.toString
        correctCount = 0
        context watch socketActor
        context.become(joined(socketActor, uid))
        if(!recoveryRunning) mediator ! Publish("join", FieldActor.Join(uid))
      case Registered(uid, name) =>
        nicName = name
        if(!recoveryRunning) mediator ! Publish("update", Updated(uid, correctCount, nicName))
      case Answered(uid, isCorrect) =>
        import context.dispatcher
        Future {
          log.info("Pi Start")
          val v = pi(piWeight)
          log.info("PI: {}", v)
        }
        correctCount = if(isCorrect) correctCount + 1 else 0
        if(!recoveryRunning) mediator ! Publish("update", Updated(uid, correctCount, nicName))
    }
  }

  import scala.annotation.tailrec
  def pi(m: Long) = {
    def leibniz(n: Long) = 4.0 * (1 - (n % 2) * 2) / (2 * n + 1)

    @tailrec
    def inner(n:Long = 0, acc: Double = 0.0): Double =
      if (n == m) acc
      else {
        inner(n + 1, acc + leibniz(n))
      }
    inner()
  }
}
