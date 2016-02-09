package com.example.calcbattle.user.actors

import akka.actor.Props
import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.UserActor.{Result, Subscribe}

object UserWorker {
  def props = Props(new UserWorker)
  val name = "UserWorker"
}
class UserWorker extends PersistentActor {
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name
  override def receiveRecover: Receive = {
    case _ =>
      println("receiveRecover")
  }
  override def receiveCommand = {
    case Subscribe(uid) =>
      println("----------------------")
      println(uid)
      println("----------------------")
    case Result(uid, isCorrect) =>
      println("----------------------")
      println(uid)
      println(isCorrect)
      println("----------------------")
  }
}
