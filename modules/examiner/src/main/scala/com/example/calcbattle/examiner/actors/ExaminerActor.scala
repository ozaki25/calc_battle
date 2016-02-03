package com.example.calcbattle.examiner.actors

import akka.actor.{Actor, Props}
import akka.cluster.Cluster

object ExaminerActor {
  def props = Props(new ExaminerActor)
  val name = "ExaminerActor"
}

class ExaminerActor extends Actor {
  def receive = {
    case _ =>
      println("examiner")
  }
}
