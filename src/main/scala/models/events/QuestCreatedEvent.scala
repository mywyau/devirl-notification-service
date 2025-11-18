package models.events

import io.circe.generic.semiauto.*
import io.circe.Decoder
import io.circe.Encoder
import java.time.Instant

final case class QuestCreatedEvent(
  questId: String,
  title: String,
  clientId: String,
  createdAt: Instant
)

object QuestCreatedEvent {
  given Encoder[QuestCreatedEvent] = deriveEncoder
  given Decoder[QuestCreatedEvent] = deriveDecoder
}
