package com.example.calcbattle.examiner.actors

import akka.actor.{Actor, Props}
import com.example.calcbattle.examiner.models.Question

object ExaminerActor {
  def props = Props(new ExaminerActor)
  val name = "ExaminerActor"
  case object Create
}

class ExaminerActor extends Actor {
  import ExaminerActor.Create

  def receive = {
    case Create =>
      sender ! Question.create()
  }
}
