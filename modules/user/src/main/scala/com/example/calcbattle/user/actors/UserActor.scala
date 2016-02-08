package com.example.calcbattle.user.actors

import akka.actor.{Actor, Props}
import com.example.calcbattle.user.actors.UserActor.{Result, Subscribe}

object UserActor {
  def props = Props(new UserActor)
  val name = "UserActor"
  class UID(val id: String) extends AnyVal
  case class Subscribe(uid: UID)
  case class Result(isCorrect: Boolean)
}

class UserActor extends Actor {
  def receive = {
    case Subscribe(uid) =>
      println("----------------------")
      println(uid)
      println("----------------------")
    case Result(isCorrect) =>
      println("----------------------")
      println(isCorrect)
      println("----------------------")
  }
}
