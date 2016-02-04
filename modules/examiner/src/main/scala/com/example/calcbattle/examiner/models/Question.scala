package com.example.calcbattle.examiner.models

import scala.util.Random

object Question {
  def create() = Question(random(), random())
  def random() = Random.nextInt(90) + 10
}

case class Question(first: Int, second: Int)
