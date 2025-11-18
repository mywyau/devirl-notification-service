package models.events

import io.circe.generic.semiauto.*
import io.circe.Decoder
import io.circe.Encoder
import java.time.Instant

final case class QuestCompletedEvent(
  questId: String,
  title: String,
  clientId: String,
  createdAt: Instant
)

object QuestCompletedEvent {
  given Encoder[QuestCompletedEvent] = deriveEncoder
  given Decoder[QuestCompletedEvent] = deriveDecoder
}
