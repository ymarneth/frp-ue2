package frp.basics.iot

import io.circe
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, parser}
import io.circe.syntax.EncoderOps

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

case class Measurement(override val id: Int, value: Double, timestamp: LocalDateTime) extends Entity(id):
  def ack(): Acknowledgement = Acknowledgement(id)

  def toJson: String = this.asJson.noSpaces
end Measurement

case object Acknowledgement:
  implicit val measurementDecoder: Decoder[Acknowledgement] = deriveDecoder[Acknowledgement]
  implicit val measurementEncoder: Encoder.AsObject[Acknowledgement] = deriveEncoder[Acknowledgement]
end Acknowledgement

case class Acknowledgement(id: Int):
  def toJson: String = this.asJson.noSpaces
end Acknowledgement