package frp.exercises.Common

import akka.Done
import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.H2Profile.api.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Success

object Repository {
  def apply(insertDuration: FiniteDuration = 100.millis) =
    new Repository(insertDuration)
}

class Repository(private val insertDuration: FiniteDuration) {
  private val system = DefaultActorSystem()
  private var tracingEnabled = false
  private val db = Database.forConfig("slick-h2.db")
  private val slickSession: SlickSession = SlickSession.forDbAndProfile(db, slick.jdbc.H2Profile)
  private val measurements = TableQuery[Measurements]

  private val createTableAction = measurements.schema.createIfNotExists
  private val tableCreationFuture: Future[Unit] = db.run(createTableAction).map(_ => println("Table 'MEASUREMENTS' created successfully")).recover {
    case exception => println(s"Failed to create table 'MEASUREMENTS': ${exception.getMessage}")
  }

  def totalBulkInsertDuration(blockSize: Int): FiniteDuration = {
    val timePerRow = blockSize match {
      case n if n < 1 => 0.millis
      case n if n <= 1 => insertDuration
      case n if n < 10 => insertDuration + (insertDuration / 10 - insertDuration) / (10 - 1) * (n - 1)
      case n if n <= 100 => insertDuration / 10
      case n => insertDuration / 10 + (insertDuration - insertDuration / 10) / (1000 - 100) * (n - 100)
    }
    timePerRow * blockSize
  }

  private def completeAfter(delay: FiniteDuration): Future[Done] = {
    val promise = Promise[Done]()
    system.scheduler.scheduleOnce(delay, () => promise.complete(Success(Done)))
    promise.future
  }

  private def traceInsert(measurement: Measurement): Measurement = {
    if (tracingEnabled) println(s"Inserting measurement with ID: ${measurement.id}, Value: ${measurement.temperature}, Timestamp: ${measurement.timestamp}")
    measurement
  }

  private def traceBulkInsert(items: Seq[Measurement]): Seq[Measurement] = {
    if (tracingEnabled) println(s"Bulk inserting measurements with IDs: [${items.map(item => item.id).mkString(", ")}]")
    items
  }

  def withTracing(enabled: Boolean = true): Repository = {
    tracingEnabled = enabled
    this
  }

  def insertAsync(measurement: Measurement): Future[Measurement] = {
    tableCreationFuture.flatMap { _ =>
      val action = measurements += measurement
      db.run(action).map(_ => traceInsert(measurement))
    }
  }

  def bulkInsertAsync(measurements: Seq[Measurement]): Future[Seq[Measurement]] = {
    tableCreationFuture.flatMap { _ =>
      val action = this.measurements ++= measurements
      db.run(action).map(_ => traceBulkInsert(measurements))
    }
  }
}
