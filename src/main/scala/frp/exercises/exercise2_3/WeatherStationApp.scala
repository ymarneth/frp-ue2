package frp.exercises.exercise2_3

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import frp.exercises.Common.{Acknowledgement, DefaultActorSystem, Measurement}
import frp.exercises.Common.MeasureUtil.measure
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}

@main
def WeatherStationApp(): Unit = {
  println("==================== WeatherStation with Typed Actors ==========================")

  val NR_MESSAGES = 200
  val MESSAGES_PER_SECOND = 100
  val PARALLELISM = Runtime.getRuntime.availableProcessors
  val BULK_SIZE = 10
  val TIME_WINDOW = 200.millis

  val actorSystem: ActorSystem[DataStorage.Command] =
    DefaultActorSystem(DataStorage(BULK_SIZE, TIME_WINDOW, PARALLELISM))

  given ExecutionContext = actorSystem.executionContext

  def measurementsServiceWithActor(dbActor: ActorRef[DataStorage.Command], parallelism: Int = PARALLELISM): Flow[String, String, NotUsed] =
    import DataStorage.Insert
    given ActorSystem[DataStorage.Command] = actorSystem

    given Timeout = 500.millis

    Flow[String]
      .mapAsync(parallelism)(json => Future {
        Measurement.fromJson(json)
      })
      .collect { case Right(meas) => meas }
      .mapAsync[Acknowledgement](parallelism * BULK_SIZE)(meas => dbActor ? (Insert(meas, _)))
      .map(ack => ack.toJson)
  end measurementsServiceWithActor

  val weatherStation = WeatherStationSimulator(NR_MESSAGES, MESSAGES_PER_SECOND).withTracing()

  val done = measure(weatherStation.handleMessages(measurementsServiceWithActor(actorSystem))){
    time => println(s"Throughput of MeasurementService: ${NR_MESSAGES / time} msg/s")
  }

  Await.ready(done, Duration.Inf)

  Await.ready(DefaultActorSystem.terminate(), Duration.Inf)

  println("============================== System Terminated ===============================")
}
