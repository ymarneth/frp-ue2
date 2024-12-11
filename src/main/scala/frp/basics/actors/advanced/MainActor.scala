package frp.basics.actors.advanced

import scala.concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, PoolRouter, Routers}
import akka.actor.typed.{Behavior, SupervisorStrategy, Terminated}

object MainActor:

  def apply() =
    Behaviors.setup { context =>

      Behaviors.same
    }
  end apply

end MainActor
