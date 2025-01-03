package frp.exercises.exercise2_3

import akka.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main
def WeatherStationApp(): Unit = {
  println("==================== WeatherStation with Typed Actors ==========================")

  val system = ActorSystem(MainActor(), "WeatherStationSystem")

  println("============================== System Terminated ===============================")
}
