package consumers

package kafka.consumers

import cats.effect.*
import fs2.kafka.*
import cats.syntax.all.*
import fs2.Stream
import io.circe.parser.decode
import io.circe.generic.auto.*
import org.typelevel.log4cats.Logger
import models.Notification
import repositories.NotificationRepositoryAlgebra
import fs2.concurrent.Topic


final class NotificationConsumer[F[_]: Async: Logger](
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
        decode[Notification](record.value) match {
          case Right(notification) =>
            for {
              _ <- Logger[F].info(s"[KafkaConsumer] Received notification for ${notification.userId}")
              _ <- repo.insert(notification)
              _ <- topic.publish1(notification)
            } yield ()
          case Left(err) =>
            Logger[F].error(s"[KafkaConsumer] Decode error: ${err.getMessage}")
        }
      }
}
