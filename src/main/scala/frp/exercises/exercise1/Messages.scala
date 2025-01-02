package frp.exercises.exercise1

import java.time.LocalDateTime

object Messages {
  case class SendMessage(id: Int, text: String)

  case class PrintMessage(id: Int, text: String, arrivalTime: LocalDateTime)
}

