package com.example.calcbattle.examiner.actors

import akka.actor.{Actor, Props}
import com.example.calcbattle.examiner.models.Question

object ExaminerActor {
  def props = Props(new ExaminerActor)
  val name = "ExaminerActor"

  case object Create
  case class Check(answer: Answer)
  case class Exam(question: Question)
  case class Result(isCorrect: Boolean)
  case class Answer(first: Int, second: Int, input: Int)
}

class ExaminerActor extends Actor {
  import ExaminerActor.{Create, Check, Exam, Result}

  def receive = {
    case Create =>
      sender ! Exam(Question.create())
    case Check(answer) =>
            sender ! Result(Question.result(answer))
  }
}
