package services

import java.time.Instant
import java.util.UUID
import models.Notification
import models.NotificationEvent
import models.NotificationEvent.*

object NotificationMapper {

  def fromEvent(event: NotificationEvent): Notification =
    event match {
      case QuestCompleted(userId, questTitle, reward) =>
        Notification(
          id = UUID.randomUUID().toString,
          userId = userId,
          title = "Quest Completed!",
          message = s"You finished '$questTitle' and earned $$${reward}.",
          eventType = "quest.completed",
          createdAt = Instant.now(),
          read = false
        )

      case QuestPaid(userId, amount) =>
        Notification(
          id = UUID.randomUUID().toString,
          userId = userId,
          title = "Payment Received",
          message = s"You were paid $$${amount}.",
          eventType = "quest.paid",
          createdAt = Instant.now(),
          read = false
        )

      case QuestUpdated(userId, questTitle) =>
        Notification(
          id = UUID.randomUUID().toString,
          userId = userId,
          title = "Quest Updated",
          message = s"The quest '$questTitle' was updated.",
          eventType = "quest.updated",
          createdAt = Instant.now(),
          read = false
        )
    }
}
