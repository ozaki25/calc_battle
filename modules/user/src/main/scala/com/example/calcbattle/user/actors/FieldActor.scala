package com.example.calcbattle.user.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.example.calcbattle.user.actors.FieldActor.UID

object FieldActor {
  def props = Props(new FieldActor)
  val name = "FieldActor"

  class UID(val id: String) extends AnyVal
  case class Subscribe(uid: UID)
}

class FieldActor extends Actor {
  import FieldActor.Subscribe

  var users = Map[ActorRef, UID]()

  def receive = {
    case Subscribe(uid) =>
      println("------fieldActor------")
      println(users)
      users += (sender -> uid)
      println(users)
      context watch sender
      println("----------------------")
    case Terminated(user) =>
      println("------fieldActor------")
      println(users)
      users -= user
      println(users)
      println("----------------------")
  }
}
