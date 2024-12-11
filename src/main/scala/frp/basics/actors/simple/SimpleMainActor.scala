package frp.basics.actors.simple

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Signal, Terminated}

object SimpleMainActor:

  def apply(): Behavior[SimplePrimeCalculator.Reply] =
    Behaviors.setup(context => new SimpleMainActor(context))

end SimpleMainActor

class SimpleMainActor(context: ActorContext[SimplePrimeCalculator.Reply])
  extends AbstractBehavior[SimplePrimeCalculator.Reply](context):

  import SimplePrimeCalculator._

  //val calc: ActorRef[Command] = context.spawn(SimplePrimeCalculator.ooBehavior(), "simple-prime-calculator")
  val calc: ActorRef[Command] = context.spawn(SimplePrimeCalculator.functionalBehavior(), "simple-prime-calculator")

  calc ! Find(2, 100, context.self)
  calc ! Find(1000, 1200, context.self)
  calc ! Shutdown

  context.watch(calc)

  override def onMessage(msg: Reply): Behavior[Reply] =
    msg match
      case Found(lower, upper, primes) =>
        println(s"Primes between $lower and $upper: [${primes.mkString(", ")}]")
    Behaviors.same

  override def onSignal: PartialFunction[Signal, Behavior[Reply]] =
    case Terminated(ref) =>
      println(s"SimpleMainActor: ${ref.path} terminated")
      context.system.terminate()
      Behaviors.stopped

end SimpleMainActor
