package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object MainActor {

  sealed trait Command

  private case object StartWeatherStation extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val dataStorage: ActorRef[DataStorage.Command] = context.spawn(DataStorage(), "DataStorage")
    val weatherStation: ActorRef[WeatherStation.Command] = context.spawn(WeatherStation(processingActor = dataStorage), "WeatherStation")

    weatherStation ! WeatherStation.StartWeatherStation

    context.children.foreach(c => context.watch(c))

    Behaviors.receiveSignal {
      case (context, Terminated(_)) =>
        if context.children.size <= 1 then
          context.log.info("All actors have stopped. Shutting down the system.")
          context.system.terminate()
          Behaviors.stopped
        else
          Behaviors.same
    }
  }
}
