package com.example.calcbattle.examiner.actors

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import com.example.calcbattle.examiner.models.Question
import com.example.calcbattle.examiner.actors.ExaminerActor._

object ExaminerActor {
  def props = Props(new ExaminerActor)
  val name = "ExaminerActor"
  case class Create()
}

class ExaminerActor extends Actor {
  def receive = {
    case Create =>
      sender() ! Question.create()
    case _ =>
      println("examiner")
  }
}
