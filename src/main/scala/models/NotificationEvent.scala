package models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

sealed trait NotificationEvent

object NotificationEvent {
    
  final case class QuestCompleted(userId: String, questTitle: String, reward: Int) extends NotificationEvent
  final case class QuestPaid(userId: String, amount: Int) extends NotificationEvent
  final case class QuestUpdated(userId: String, questTitle: String) extends NotificationEvent

  implicit val questCompletedDecoder: Decoder[QuestCompleted] = deriveDecoder
  implicit val questPaidDecoder: Decoder[QuestPaid] = deriveDecoder
  implicit val questUpdatedDecoder: Decoder[QuestUpdated] = deriveDecoder

  implicit val questCompletedEncoder: Encoder[QuestCompleted] = deriveEncoder
  implicit val questPaidEncoder: Encoder[QuestPaid] = deriveEncoder
  implicit val questUpdatedEncoder: Encoder[QuestUpdated] = deriveEncoder

  // 👇 Important: polymorphic encoder/decoder for sealed trait
  implicit val notificationEventEncoder: Encoder[NotificationEvent] = deriveEncoder
  implicit val notificationEventDecoder: Decoder[NotificationEvent] = deriveDecoder
}
