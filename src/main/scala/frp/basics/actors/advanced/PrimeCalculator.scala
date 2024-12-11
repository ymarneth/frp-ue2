package frp.basics.actors.advanced

import scala.concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object PrimeCalculator:
  sealed trait Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context => new PrimeCalculator(context) }
  end apply
end PrimeCalculator

class PrimeCalculator(context: ActorContext[PrimeCalculator.Command])
  extends AbstractBehavior[PrimeCalculator.Command](context):

  override def onMessage(msg: PrimeCalculator.Command): Behavior[PrimeCalculator.Command] = ???

end PrimeCalculator
