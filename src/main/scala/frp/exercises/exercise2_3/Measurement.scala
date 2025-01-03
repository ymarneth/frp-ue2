package frp.exercises.exercise2_3

import io.circe
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, parser}
import io.circe.syntax.EncoderOps
import slick.jdbc.H2Profile.api.*

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Measurement(override val id: Int, temperature: DegreesCelsius, timestamp: LocalDateTime) extends Entity(id)

case object Acknowledgement:
  implicit val measurementDecoder: Decoder[Acknowledgement] = deriveDecoder[Acknowledgement]
  implicit val measurementEncoder: Encoder.AsObject[Acknowledgement] = deriveEncoder[Acknowledgement]
end Acknowledgement

case class Acknowledgement(id: Int):
  def toJson: String = this.asJson.noSpaces
end Acknowledgement

class Measurements(tag: Tag) extends Table[Measurement](tag, "MEASUREMENTS") {
  def id = column[Int]("ID", O.PrimaryKey)
  def value = column[Double]("VALUE")
  def timestamp = column[Long]("TIMESTAMP")

  def * = (id, value, timestamp) <> (
    (id: Int, value: Double, timestamp: Long) => Measurement(id, value, LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC)),
    (m: Measurement) => Some((m.id, m.temperature, m.timestamp.toEpochSecond(java.time.ZoneOffset.UTC)))
  )
}

type DegreesCelsius = Double
