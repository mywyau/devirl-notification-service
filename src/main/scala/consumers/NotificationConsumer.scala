package consumers

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.kafka.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import java.time.Instant
import java.util.UUID
import models.IncomingKafkaEvent
import models.Notification
import models.NotificationEvent
import models.NotificationEvent.*
import org.typelevel.log4cats.Logger
import repositories.NotificationRepositoryAlgebra
import scala.concurrent.duration.*

final class NotificationConsumer[F[_] : Async : Temporal : Logger](
  topic: Topic[F, Notification],
  repo: NotificationRepositoryAlgebra[F],
  kafkaConfig: ConsumerSettings[F, String, String]
) {

  private val topicName = "quest.events.v1"

  private def process(notification: Notification) =
    for {
      _ <- repo.insert(notification)
      _ <- topic.publish1(notification)
      _ <- Logger[F].info(s"[KafkaConsumer] Sent to ${notification.userId}")
    } yield ()

  def stream: Stream[F, Unit] =
    KafkaConsumer
      .stream(kafkaConfig)
      .subscribeTo(topicName)
      .records
      .evalMap { committable =>

        val record = committable.record

        decode[IncomingKafkaEvent](record.value) match {
          case Right(IncomingKafkaEvent.QuestCompleted(event)) =>
            val notification = Notification(
              notificationId = UUID.randomUUID().toString,
              userId = event.clientId,
              title = event.title,
              message = s"You finished ${event.title}",
              eventType = "quest.completed",
              createdAt = Instant.now(),
              read = false
            )
            process(notification).as(committable.offset)

          case Right(IncomingKafkaEvent.QuestCreated(event)) =>
            val notification =
              Notification(
                notificationId = UUID.randomUUID().toString,
                userId = event.clientId,
                title = event.title,
                message = "Quest created with title Mikey",
                eventType = "quest.completed",
                createdAt = Instant.now(),
                read = false
              )
            process(notification).as(committable.offset)

          case Right(IncomingKafkaEvent.QuestUpdated(event)) =>
            val notification =
              Notification(
                notificationId = UUID.randomUUID().toString,
                userId = event.clientId,
                title = event.title,
                message = s"The quest '$event.title' was updated.",
                eventType = "quest.updated",
                createdAt = Instant.now(),
                read = false
              )
            process(notification).as(committable.offset)
          case Left(err) =>
            Logger[F].error(s"[KafkaConsumer] Decode error: ${err.getMessage}").as(committable.offset)
        }
      }
      .through(commitBatchWithin(100, 10.seconds))
}
