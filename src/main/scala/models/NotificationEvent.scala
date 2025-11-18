package models

import cats.syntax.all.toFunctorOps
import io.circe.generic.semiauto.*
import io.circe.Decoder
import io.circe.Encoder

sealed trait NotificationEvent

object NotificationEvent {
  
  final case class QuestCompleted(userId: String, questTitle: String, reward: Int) extends NotificationEvent
  final case class QuestPaid(userId: String, amount: Int) extends NotificationEvent
  final case class QuestUpdated(userId: String, questTitle: String) extends NotificationEvent

  // 🔧 Custom flat decoder/encoder instead of tagged one
  implicit val questCompletedDecoder: Decoder[QuestCompleted] = deriveDecoder
  implicit val questPaidDecoder: Decoder[QuestPaid] = deriveDecoder
  implicit val questUpdatedDecoder: Decoder[QuestUpdated] = deriveDecoder

  implicit val questCompletedEncoder: Encoder[QuestCompleted] = deriveEncoder
  implicit val questPaidEncoder: Encoder[QuestPaid] = deriveEncoder
  implicit val questUpdatedEncoder: Encoder[QuestUpdated] = deriveEncoder

  // 🔥 Smart decoder that figures out which one it is based on fields
  implicit val notificationEventDecoder: Decoder[NotificationEvent] =
    List[Decoder[NotificationEvent]](
      questCompletedDecoder.widen,
      questPaidDecoder.widen,
      questUpdatedDecoder.widen
    ).reduceLeft(_ or _)

  implicit val notificationEventEncoder: Encoder[NotificationEvent] = Encoder.instance {
    case e: QuestCompleted => questCompletedEncoder(e)
    case e: QuestPaid => questPaidEncoder(e)
    case e: QuestUpdated => questUpdatedEncoder(e)
  }
}
