package models

import io.circe.Decoder
import models.events.*

sealed trait IncomingKafkaEvent

object IncomingKafkaEvent {

  case class QuestCompleted(event: QuestCompletedEvent) extends IncomingKafkaEvent
  case class QuestCreated(event: QuestCreatedEvent) extends IncomingKafkaEvent
  case class QuestUpdated(event: QuestUpdatedEvent) extends IncomingKafkaEvent

  // Smart decoder that tries all event types
  implicit val decoder: Decoder[IncomingKafkaEvent] =
    List[Decoder[IncomingKafkaEvent]](
      Decoder[QuestCompletedEvent].map(QuestCompleted.apply),
      Decoder[QuestCreatedEvent].map(QuestCreated.apply),
      Decoder[QuestUpdatedEvent].map(QuestUpdated.apply)
    ).reduceLeft(_ or _)
}
