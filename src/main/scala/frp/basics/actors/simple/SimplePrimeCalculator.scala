package frp.basics.actors.simple

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import frp.basics.actors.PrimeUtil.isPrime

object SimplePrimeCalculator:

  sealed trait Command

  final case class Find(lower: Int, upper: Int, replyTo: ActorRef[Reply]) extends Command

  case object Shutdown extends Command

  sealed trait Reply

  final case class Found(lower: Int, upper: Int, primes: Seq[Int]) extends Reply

  def ooBehavior(): Behavior[Command] =
    Behaviors.setup(context => new SimplePrimeCalculator(context))

  def functionalBehavior(): Behavior[Command] =
    Behaviors.receiveMessage{
      case Find(lower, upper, replyTo) =>
        val primes = (lower to upper).filter(isPrime)
        replyTo ! Found(lower, upper, primes)
        Behaviors.same
      case Shutdown => Behaviors.stopped
    }

end SimplePrimeCalculator

class SimplePrimeCalculator(context: ActorContext[SimplePrimeCalculator.Command])
  extends AbstractBehavior[SimplePrimeCalculator.Command](context):

  import SimplePrimeCalculator._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match
      case Find(lower, upper, replyTo) =>
        val primes = (lower to upper).filter(isPrime)
        replyTo ! Found(lower, upper, primes)
        Behaviors.same
      case Shutdown => Behaviors.stopped

end SimplePrimeCalculator
