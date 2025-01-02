package frp.exercises.exercise2_1

import akka.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main
def messageExchangeMain(): Unit = {
  println("==================== MessageSenderApp ==========================")

  val system = ActorSystem(MainActor(), "main-actor")
  
  Thread.sleep(3000)
  system.terminate()
  Await.ready(system.whenTerminated, Duration.Inf)

  println("==================== System Terminated ==========================")
}
