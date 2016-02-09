package com.example.calcbattle.user.actors.UserWorker

import akka.persistence.PersistentActor
import com.example.calcbattle.user.actors.UserActor.{UID, Subscribe, Result}

object UserWorker
class UserWorker(uid: UID) extends PersistentActor {
  override def persistenceId: String = uid.id
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
