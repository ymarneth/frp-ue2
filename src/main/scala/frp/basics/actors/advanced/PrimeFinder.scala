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

  private def failSometimes(probability: Double, actorName: String): Unit =
    if Random.nextDouble() < probability then
      println(s"  $actorName (Thread: ${Thread.currentThread().threadId()}) FAILED)")
      throw new ArithmeticException(s"  Primefinder failed! $actorName (Thread: ${Thread.currentThread().threadId()}) FAILED)")
    end if

  def apply(): Behavior[Command] =
    Behaviors
      .receive[Command] {
        case (context, Find(lower, upper, replyTo)) =>
          println(s"  ${context.self.path.name} (Thread: ${Thread.currentThread().threadId()}) START)")

          val primes = (lower to upper) filter isPrime

          failSometimes(0.4, context.self.path.name)

          println(s"  ${context.self.path.name} (Thread: ${Thread.currentThread().threadId()}) SUCCEEDED) [$lower, $upper] => ${primes.mkString(", ")}")

          replyTo ! PartialResult(lower, upper, primes)
          Behaviors.same
      }
      .receiveSignal {
        case (context, PreRestart) =>
          println(s"  ${context.self.path.name} (Thread: ${Thread.currentThread().threadId()}) RESTARTED)")
          Behaviors.same
        case (context, PostStop) =>
          println(s"  ${context.self.path.name} (Thread: ${Thread.currentThread().threadId()}) STOPPED)")
          Behaviors.same
      }
  end apply
end PrimeFinder