package frp.basics.iot

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import frp.basics.DefaultActorSystem
import frp.basics.MeasureUtil.measure

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

@main
def ioTApp(): Unit =
  val NR_MESSAGES = 200
  val MESSAGES_PER_SECOND = 100
  val PARALLELISM = Runtime.getRuntime.availableProcessors
  val BULK_SIZE = 10
  val TIME_WINDOW = 200.millis

  val actorSystem: ActorSystem[DatabaseActor.Command] =
    DefaultActorSystem(DatabaseActor(BULK_SIZE, TIME_WINDOW, PARALLELISM))
  given ExecutionContext = actorSystem.executionContext

  def identityService(): Flow[String, String, NotUsed] = {
    Flow[String].map(identity)
  }

  def simpleMeasurementsService(parallelism: Int = PARALLELISM): Flow[String, String, NotUsed] =
    val repository = Repository().withTracing()

    Flow[String]
      .mapAsync(parallelism)(json => Future { Measurement.fromJson(json) })
      .collect { case Right(meas) => meas}
      .mapAsync(parallelism)(repository.insertAsync)
      .map(meas => meas.ack().toJson)


  def measurementsServiceWithActor(dbActor: ActorRef[DatabaseActor.Command], parallelism: Int = PARALLELISM): Flow[String, String, NotUsed] =
    import DatabaseActor.Insert
    given ActorSystem[DatabaseActor.Command] = actorSystem
    given Timeout = 500.millis

    Flow[String]
      .mapAsync(parallelism)(json => Future { Measurement.fromJson(json) })
      .collect { case Right(meas) => meas}
      .mapAsync[Acknowledgement](parallelism*BULK_SIZE)(meas => dbActor ? (Insert(meas, _)))
      .map(ack => ack.toJson)

  end measurementsServiceWithActor

  def measurementsService(parallelism: Int = PARALLELISM): Flow[String, String, NotUsed] =
    val repository = Repository().withTracing()

    Flow[String]
      .mapAsync(parallelism)(json => Future { Measurement.fromJson(json) })
      .collect { case Right(meas) => meas}
      .groupedWithin(BULK_SIZE, TIME_WINDOW)
      .mapAsync(parallelism)(measurements => repository.bulkInsertAsync(measurements))
      .mapConcat(_.map(meas => meas.ack().toJson))

  end measurementsService

  val server = ServerSimulator(NR_MESSAGES, MESSAGES_PER_SECOND).withTracing()

  //println("Starting server simulation")
  //val done1 = server.handleMessages(simpleMeasurementsService())
  //Await.ready(done1, Duration.Inf)

  //println("Starting server simulation with actor measurement service")
  //val done2 = server.handleMessages(measurementsServiceWithActor(actorSystem))
  //Await.ready(done2, Duration.Inf)

  //println("Starting server simulation with bulk insert")
  //val done3 = server.handleMessages(measurementsService())
  //Await.ready(done3, Duration.Inf)

  val done = measure(server.handleMessages(measurementsService())){
    time => println(s"Throughput of MeasurementService: ${NR_MESSAGES / time} msg/s")
  }
  Await.ready(done, Duration.Inf)

  Await.ready(DefaultActorSystem.terminate(), Duration.Inf)
end ioTApp