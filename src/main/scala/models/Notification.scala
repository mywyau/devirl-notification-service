package models

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.*
import io.circe.Decoder
import io.circe.Encoder
import java.time.Instant

case class Notification(
  id: String,
  userId: String,
  title: String,
  message: String,
  eventType: String,
  createdAt: Instant,
  read: Boolean
)

object Notification {

  implicit val encoder: Encoder[Notification] = deriveEncoder[Notification]
  implicit val decoder: Decoder[Notification] = deriveDecoder[Notification]

}
