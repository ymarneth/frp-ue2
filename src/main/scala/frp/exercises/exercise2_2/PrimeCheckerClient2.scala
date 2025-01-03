package frp.exercises.exercise2_2

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import scala.util.{Failure, Success}
import scala.concurrent.duration.*

object PrimeCheckerClient2 {

  sealed trait Command

  case object StartProcessing extends Command

  case object ProcessNext extends Command

  private final case class WrappedPrimeResult(reply: PrimeChecker.Reply) extends Command

  private case class FailedResponse(value: Int, message: String) extends Command

  private val TIMEOUT = 50.millis

  given Timeout = TIMEOUT

  def apply(primeChecker: ActorRef[PrimeChecker.Command], numbers: List[Int]): Behavior[Command] =
    Behaviors.withTimers { timers =>
      active(timers, primeChecker, numbers)
    }

  private def active(timers: TimerScheduler[Command],
                     primeChecker: ActorRef[PrimeChecker.Command],
                     numbers: List[Int]): Behavior[Command] =
  Behaviors.receive[Command] {
      case (context, StartProcessing) =>
        context.log.info("Starting to process the list of numbers.")
        println("Starting to process the list of numbers.")
        timers.startSingleTimer(ProcessNext, 10.millis)
        Behaviors.same

      case (context, ProcessNext) =>
        numbers match {
          case Nil =>
            context.log.info("All numbers processed. Shutting down.")
            println("All numbers processed. Shutting down.")
            Behaviors.stopped

          case head :: tail =>
            context.log.info(s"Sending number $head to PrimeChecker")
            println(s"Sending number $head to PrimeChecker")
            context.ask(primeChecker, (ref: ActorRef[PrimeChecker.Reply]) => PrimeChecker.CheckIfPrime(head, ref)) {
              case Success(value) => WrappedPrimeResult(value)
              case Failure(_) => FailedResponse(head, s"Got no reply from PrimeChecker within the timeout ($TIMEOUT)")
            }
            timers.startSingleTimer(ProcessNext, 50.millis)
            active(timers, primeChecker, tail)
        }

      case (context, WrappedPrimeResult(reply)) =>
        reply match
          case PrimeChecker.PrimeResult(number, factors, isPrime) =>
            context.log.info(s"Result for $number: factors = $factors, isPrime = $isPrime")
            println(s"Result for $number: factors = $factors, isPrime = $isPrime")
          case PrimeChecker.FactorizationFailure(number, reason) =>
            context.log.error(s"Failed to factorize $number: $reason")
            println(s"Failed to factorize $number: $reason")
        Behaviors.same

      case (context, FailedResponse(value, message)) =>
        context.log.error(s"Failed to factorize: $message")
        println(s"Failed to test $value for prime: $message")
        Behaviors.same
    }
}