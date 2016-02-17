package com.example.calcbattle.examiner.models

import scala.util.Random
import com.example.calcbattle.examiner.actors.ExaminerActor.Answer

object Question {
  def create() = Question(random(), random())
  def random() = Random.nextInt(90) + 10
  def result(answer: Answer) = answer.input == answer.first + answer.second
}

case class Question(first: Int, second: Int)
