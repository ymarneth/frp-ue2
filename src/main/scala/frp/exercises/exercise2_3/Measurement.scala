package frp.exercises.exercise2_3

import java.time.LocalDateTime

object TemperatureMeasurement:
end TemperatureMeasurement

case class TemperatureMeasurement(override val id: Int, value: Double, timestamp: LocalDateTime) extends Entity(id):
  
end TemperatureMeasurement