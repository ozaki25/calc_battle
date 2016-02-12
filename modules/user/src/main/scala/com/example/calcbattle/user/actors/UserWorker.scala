package com.example.calcbattle.user.actors

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.FieldActor.Subscribe

object UserWorker {
  def props(field: ActorRef) = Props(new UserWorker(field))
  val name = "UserWorker"
}
class UserWorker(field: ActorRef) extends PersistentActor {
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
  override def receiveRecover: Receive = {
    case _ =>
      println("receiveRecover")
  }
  override def receiveCommand = {
    case Subscribe(uid) =>
      println("------userWorker------")
      field forward Subscribe(uid)
      println("----------------------")
  }
}
