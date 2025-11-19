package models

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import models.events.*

case class IncomingEvent(`type`: String, payload: Json)

object IncomingEvent {

  implicit val encoder: Encoder[IncomingEvent] = deriveEncoder[IncomingEvent]
  implicit val decoder: Decoder[IncomingEvent] = deriveDecoder[IncomingEvent]
}

// object IncomingEvent {

//   case class QuestCompleted(event: QuestCompletedEvent) extends IncomingKafkaEvent
//   case class QuestCreated(event: QuestCreatedEvent) extends IncomingKafkaEvent
//   case class QuestUpdated(event: QuestUpdatedEvent) extends IncomingKafkaEvent

//   // Smart decoder that tries all event types
//   implicit val decoder: Decoder[IncomingKafkaEvent] =
//     List[Decoder[IncomingKafkaEvent]](
//       Decoder[QuestCompletedEvent].map(QuestCompleted.apply),
//       Decoder[QuestCreatedEvent].map(QuestCreated.apply),
//       Decoder[QuestUpdatedEvent].map(QuestUpdated.apply)
//     ).reduceLeft(_ or _)
// }
