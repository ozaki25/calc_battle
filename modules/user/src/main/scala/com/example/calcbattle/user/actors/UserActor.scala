package com.example.calcbattle.user.actors

import akka.actor.{Actor, Props}

object UserActor {
  def props = Props(new UserActor)
  val name = "UserActor"
}

class UserActor extends Actor {
  def receive = {
    case a: String =>
      println("----------------------")
      println(a)
      println("----------------------")
  }
}
