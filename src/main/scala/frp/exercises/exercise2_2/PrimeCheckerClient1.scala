package frp.exercises.exercise2_2

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._

object PrimeCheckerClient1 {

  sealed trait Command

  case object StartProcessing extends Command

  private case object ProcessNext extends Command

  private final case class WrappedPrimeResult(reply: PrimeChecker.Reply) extends Command

  def apply(primeChecker: ActorRef[PrimeChecker.Command], numbers: List[Int]): Behavior[Command] =
    Behaviors.withTimers { timers =>
      active(timers, primeChecker, numbers)
    }

  private def active(timers: TimerScheduler[Command],
                     primeChecker: ActorRef[PrimeChecker.Command],
                     numbers: List[Int]
                    ): Behavior[Command] =
    Behaviors.setup { context =>
      val adapter: ActorRef[PrimeChecker.Reply] = context.messageAdapter(WrappedPrimeResult.apply)

      Behaviors.receiveMessage {
        case StartProcessing =>
          context.log.info("Starting to process the list of numbers.")
          println("Starting to process the list of numbers.")
          timers.startSingleTimer(ProcessNext, 10.millis)
          Behaviors.same

        case ProcessNext =>
          numbers match {
            case Nil =>
              context.log.info("All numbers processed. Shutting down.")
              println("All numbers processed. Shutting down.")
              Behaviors.stopped

            case head :: tail =>
              context.log.info(s"Sending number $head to PrimeChecker")
              println(s"Sending number $head to PrimeChecker")
              primeChecker ! PrimeChecker.CheckIfPrime(head, adapter)
              timers.startSingleTimer(ProcessNext, 50.millis)
              active(timers, primeChecker, tail)
          }

        case WrappedPrimeResult(reply) =>
          reply match
            case PrimeChecker.PrimeResult(number, factors, isPrime) =>
              context.log.info(s"Result for $number: factors = $factors, isPrime = $isPrime")
              println(s"Result for $number: factors = $factors, isPrime = $isPrime")
            case PrimeChecker.FactorizationFailure(number, reason) =>
              context.log.error(s"Failed to factorize $number: $reason")
              println(s"Failed to factorize $number: $reason")
          Behaviors.same
      }
    }
}
