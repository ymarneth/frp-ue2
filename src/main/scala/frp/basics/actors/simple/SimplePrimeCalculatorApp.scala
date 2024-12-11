package frp.basics.actors.simple

import akka.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main
def simplePrimeCalculatorMain(): Unit =

  println("==================== SimplePrimeCalculatorApp ==========================")

  val system = ActorSystem(SimpleMainActor(), "simple-prime-calculator-system")

  Await.result(system.whenTerminated, Duration.Inf)

end simplePrimeCalculatorMain