package frp.exercises.exercise2_2

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

@main
def primeCheckerMain(): Unit = {
 //println("==================== PrimeCheckerApp with Client 1 ==========================")

 //val rootBehavior: Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
 //  val primeChecker = context.spawn(PrimeChecker(), "prime-checker")

 //  val client = context.spawn(
 //    PrimeCheckerClient1(primeChecker, List(2, 3, 4, 5, 6, 7, 8, 9, 10)),
 //    "prime-checker-client"
 //  )

 //  client ! PrimeCheckerClient1.StartProcessing

 //  Behaviors.empty
 //}

 //val system: ActorSystem[Nothing] = ActorSystem(rootBehavior, "prime-checker-system")

 //Thread.sleep(5000)

 //system.terminate()

 //println("==================== System Terminated ==========================")

 //println("==================== PrimeCheckerApp with Client 2 ==========================")

  val rootBehavior2: Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val primeChecker = context.spawn(PrimeChecker(), "prime-checker-2")

    val client = context.spawn(
      PrimeCheckerClient2(primeChecker, List(2, 3, 4, 5, 6, 7, 8, 9, 10)),
      "prime-checker-client-2"
    )

    client ! PrimeCheckerClient2.StartProcessing

    Behaviors.empty
  }

  val system2: ActorSystem[Nothing] = ActorSystem(rootBehavior2, "prime-checker-system-2")

  Thread.sleep(5000)

  system2.terminate()

  println("==================== System Terminated ==========================")
}