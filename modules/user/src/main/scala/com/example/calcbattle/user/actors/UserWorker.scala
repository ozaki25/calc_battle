package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"
}
class UserWorker(field: ActorRef) extends PersistentActor {
  import com.example.calcbattle.user.actors.FieldActor.Join
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.Publish


  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
  override def receiveRecover: Receive = {
    case _ =>
      println("receiveRecover")
  }
  override def receiveCommand = {
    case Join(uid) =>
      println("------userWorker------")
      val mediator = DistributedPubSub(context.system).mediator
      mediator ! Publish("userJoin", Join(uid))
      println("----------------------")
  }
}
