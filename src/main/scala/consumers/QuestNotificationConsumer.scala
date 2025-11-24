package consumers

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.kafka.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.Json
import java.time.Instant
import java.util.UUID
import models.events.QuestCompletedEvent
import models.events.QuestCreatedEvent
import models.events.QuestUpdatedEvent
import models.Envelope
import models.EventEnvelope
import models.IncomingEvent
import models.Notification
import models.NotificationEvent
import models.NotificationEvent.*
import org.typelevel.log4cats.Logger
import repositories.*
import scala.concurrent.duration.*

final class QuestNotificationConsumer[F[_] : Async : Temporal : Logger](
  topicNameSubscribedTo: String,
  topic: Topic[F, Notification],
  repo: NotificationsRepositoryAlgebra[F],
  kafkaConfig: ConsumerSettings[F, String, String]
) {

  private def process(notification: Notification, offset: CommittableOffset[F]): F[CommittableOffset[F]] =
    for {
      _ <- repo.insert(notification)
      _ <- topic.publish1(notification)
      _ <- Logger[F].info(s"[NotificationConsumer] Sent to ${notification.userId}")
    } yield offset

  // -------------------------
  // Event Handlers
  // -------------------------

  private def handleCompleted(e: QuestCompletedEvent): Notification =
    Notification(
      notificationId = UUID.randomUUID().toString,
      userId = e.clientId,
      title = e.title,
      message = s"You finished ${e.title}",
      eventType = "quest.completed",
      createdAt = Instant.now(),
      read = false
    )

  private def handleCreated(e: QuestCreatedEvent): Notification =
    Notification(
      notificationId = UUID.randomUUID().toString,
      userId = e.clientId,
      title = e.title,
      message = s"New quest created: ${e.title}",
      eventType = "quest.created",
      createdAt = Instant.now(),
      read = false
    )

  private def handleUpdated(e: QuestUpdatedEvent): Notification =
    Notification(
      notificationId = UUID.randomUUID().toString,
      userId = e.clientId,
      title = e.title,
      message = s"The quest '${e.title}' was updated.",
      eventType = "quest.updated",
      createdAt = Instant.now(),
      read = false
    )

  def stream: Stream[F, Unit] =
    KafkaConsumer
      .stream(kafkaConfig)
      .subscribeTo(topicNameSubscribedTo)
      .records
      .evalMap { committable =>

        decode[Envelope[Json]](committable.record.value) match {

          case Right(Envelope("quest.completed", payload)) =>
            payload.as[QuestCompletedEvent] match {
              case Right(ev) => process(handleCompleted(ev), committable.offset)
              case Left(err) => Logger[F].error(err.getMessage).as(committable.offset)
            }

          case Right(Envelope("quest.created", payload)) =>
            payload.as[QuestCreatedEvent] match {
              case Right(ev) => process(handleCreated(ev), committable.offset)
              case Left(err) => Logger[F].error(err.getMessage).as(committable.offset)
            }

          case Right(Envelope("quest.updated", payload)) =>
            payload.as[QuestUpdatedEvent] match {
              case Right(ev) => process(handleUpdated(ev), committable.offset)
              case Left(err) => Logger[F].error(err.getMessage).as(committable.offset)
            }

          case Right(other) =>
            Logger[F].error(s"Unknown event type: ${other.typeName}").as(committable.offset)

          case Left(err) =>
            Logger[F]
              .error(s"Failed to decode envelope: ${err.getMessage}")
              .as(committable.offset)
        }
      }
      .through(commitBatchWithin(100, 10.seconds))
}
