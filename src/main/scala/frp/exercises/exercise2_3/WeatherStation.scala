package frp.exercises.exercise2_3

import akka.actor.typed.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import frp.exercises.Common.{DefaultActorSystem, Measurement}
import frp.exercises.Common.LogUtil.tracef

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

trait WeatherStation:
  def handleMessages(service: Flow[String, String, NotUsed]): Future[Done]
end WeatherStation

object WeatherStationSimulator:
  def apply(nrMeasurements: Int = Int.MaxValue, measurementsPerSecond: Int = 20) =
    new WeatherStationSimulator(nrMeasurements, measurementsPerSecond)
end WeatherStationSimulator

class WeatherStationSimulator(nrMeasurements: Int, measurementsPerSecond: Int) extends WeatherStation:
  given ActorSystem[Nothing] = DefaultActorSystem()

  private var tracingEnabled = false;

  def withTracing(enabled: Boolean = true): WeatherStationSimulator =
    tracingEnabled = enabled
    this
  end withTracing

  override def handleMessages(service: Flow[String, String, NotUsed]): Future[Done] = {
    val source =
      Source(1 to nrMeasurements)
        .throttle(measurementsPerSecond, 1.second)
        .map(id => Measurement(id, Random.between(-20, 40), LocalDateTime.now))
        .map(meas => meas.toJson)

    val sink = Sink.ignore

    val tapIn = Flow[String].wireTap(req => tracef(req, "--> %s", tracingEnabled))
    val tapOut = Flow[String].wireTap(res => tracef(res, "<-- %s", tracingEnabled))

    source.via(tapIn).via(service).via(tapOut).runWith(sink)
  }
end WeatherStationSimulator
