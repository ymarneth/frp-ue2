package frp.basics.actors.advanced

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart}
import frp.basics.actors.PrimeUtil.isPrime

import scala.util.Random

object PrimeFinder:

  sealed trait Command
  final case class Find(lower: Int, upper: Int, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply
  final case class PartialResult(lower: Int, upper: Int, primes: Seq[Int]) extends Reply

  def apply(): Behavior[Command] =
    Behaviors
      .receive[Command] {
        case (context, Find(lower, upper, replyTo)) =>
          val primes = (lower to upper) filter isPrime
          replyTo ! PartialResult(lower, upper, primes)
          Behaviors.same
      }
  end apply
end PrimeFinder