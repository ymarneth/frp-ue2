package frp.exercises.Common

import io.circe
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, parser}
import slick.jdbc.H2Profile.api.*

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Measurement:
  implicit val measurementDecoder: Decoder[Measurement] = deriveDecoder[Measurement]
  implicit val measurementEncoder: Encoder.AsObject[Measurement] = deriveEncoder[Measurement]

  def fromJson(jsonString: String): Either[circe.Error, Measurement] =
    parser.decode[Measurement](jsonString)

  def fromJsonAsync(jsonString: String): Future[Measurement] =
    Future(fromJson(jsonString))
      .flatMap {
        case Right(measurement) => Future.successful(measurement)
        case Left(error) => Future.failed(error)
      }
end Measurement

case class Measurement(id: Int, temperature: DegreesCelsius, timestamp: LocalDateTime):
  def ack(): Acknowledgement = Acknowledgement(id)

  def toJson: String = this.asJson.noSpaces
end Measurement

class Measurements(tag: Tag) extends Table[Measurement](tag, "MEASUREMENTS") {
  def id = column[Int]("ID", O.PrimaryKey)

  def value = column[Double]("VALUE")

  def timestamp = column[Long]("TIMESTAMP")

  def * = (id, value, timestamp) <> (
    (id: Int, value: Double, timestamp: Long) => Measurement(id, value, LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC)),
    (m: Measurement) => Some((m.id, m.temperature, m.timestamp.toEpochSecond(java.time.ZoneOffset.UTC)))
  )
}

case object Acknowledgement:
  implicit val measurementDecoder: Decoder[Acknowledgement] = deriveDecoder[Acknowledgement]
  implicit val measurementEncoder: Encoder.AsObject[Acknowledgement] = deriveEncoder[Acknowledgement]
end Acknowledgement

case class Acknowledgement(id: Int):
  def toJson: String = this.asJson.noSpaces
end Acknowledgement

type DegreesCelsius = Double
