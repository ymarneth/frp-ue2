package frp.basics.actors.advanced

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import akka.actor.typed.{Behavior, SupervisorStrategy, Terminated}

object MainActor:

  import PrimeCalculator.{Failed, Result}

  private def createPrimeFinderPool(): Behavior[PrimeFinder.Command] =
    Routers.pool(poolSize = 4) {
      Behaviors.supervise(PrimeFinder())
        .onFailure[Exception](SupervisorStrategy.restart)
    }

  def apply(): Behavior[PrimeCalculator.Reply] =
    Behaviors.setup { context =>
      val workerPool = context.spawn(createPrimeFinderPool(), "worker-pool")
      context.spawn(PrimeCalculator(2, 100, context.self, workerPool), "prime-calculator-1")
      context.spawn(PrimeCalculator(2, 100, context.self, workerPool), "prime-calculator-2")

      context.children.foreach(c => context.watch(c))

      Behaviors.receiveMessage[PrimeCalculator.Reply] {
        case Result(lower, upper, primes) =>
          println(s"Primes in range $lower to $upper: ${primes.mkString(", ")}")
          Behaviors.same
        case Failed(lower, upper, reason) =>
          println(s"Failed to calculate primes in range $lower to $upper: $reason")
          Behaviors.same
      }
      .receiveSignal {
        case (context, Terminated(_)) =>
          if context.children.size <= 1 then
            context.log.info("All prime calculators have stopped. Shutting down the system.")
            context.system.terminate()
            Behaviors.stopped
          else
            Behaviors.same
      }
    }
  end apply

end MainActor
