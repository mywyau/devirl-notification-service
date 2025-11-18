package models.events

import io.circe.generic.semiauto.*
import io.circe.Decoder
import io.circe.Encoder
import java.time.Instant

final case class QuestUpdatedEvent(
  questId: String,
  title: String,
  clientId: String,
  createdAt: Instant
)

object QuestUpdatedEvent {
  given Encoder[QuestUpdatedEvent] = deriveEncoder
  given Decoder[QuestUpdatedEvent] = deriveDecoder
}
