package models

import play.api.libs.json.Json
import scala.util.Random

object Question {
  def create() = Question(random(), random())
  def random() = Random.nextInt(90) + 10

  implicit val writer = Json.writes[Question]
}


case class Question(a: Int, b: Int)
