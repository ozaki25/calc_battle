package actors

import akka.actor.Actor
import akka.cluster.Cluster
import models.Question

object ExaminerActor {


}

class ExaminerActor extends Actor {
  def receive = {
    case _ =>
      println("examiner")
  }
}
