package frp.exercises.exercise2_2

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

@main
def primeCheckerMain(): Unit = {
  println("==================== PrimeCheckerApp ==========================")

  val rootBehavior: Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val primeChecker = context.spawn(PrimeChecker(), "prime-checker")

    val client = context.spawn(
      PrimeCheckerClient1(primeChecker, List(2, 3, 4, 5, 6, 7, 8, 9, 10)),
      "prime-checker-client"
    )

    client ! PrimeCheckerClient1.StartProcessing

    Behaviors.empty
  }

  val system: ActorSystem[Nothing] = ActorSystem(rootBehavior, "prime-checker-system")

  Thread.sleep(5000)

  system.terminate()

  println("==================== System Terminated ==========================")
}