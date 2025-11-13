package consumers

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.kafka.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import models.Notification
import models.NotificationEvent
import org.typelevel.log4cats.Logger
import repositories.NotificationRepositoryAlgebra
import scala.concurrent.duration.*
import services.NotificationMapper

final class NotificationConsumer[F[_] : Async : Logger](
  topic: Topic[F, Notification],
  repo: NotificationRepositoryAlgebra[F],
  kafkaConfig: ConsumerSettings[F, String, String]
) {

  private val topicName = "notifications"

  def stream: Stream[F, Unit] =
    KafkaConsumer
      .stream(kafkaConfig)
      .subscribeTo(topicName)
      .records
      .evalMap { committable =>
        val record = committable.record

        decode[NotificationEvent](record.value) match {
          case Right(event) =>
            val notification = NotificationMapper.fromEvent(event)
            for {
              _ <- Logger[F].info(s"[KafkaConsumer] Received ${notification.eventType} for ${notification.userId}")
              _ <- repo.insert(notification)
              _ <- topic.publish1(notification)
            } yield committable.offset

          case Left(err) =>
            Logger[F].error(s"[KafkaConsumer] Decode error: ${err.getMessage}").as(committable.offset)
        }
      }
      .through(commitBatchWithin(100, 10.seconds))
}
