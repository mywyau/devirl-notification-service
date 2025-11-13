package consumers

package kafka.consumers

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.kafka.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import models.Notification
import org.typelevel.log4cats.Logger
import repositories.NotificationRepositoryAlgebra
import scala.concurrent.duration.*

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
        decode[Notification](record.value) match {
          case Right(notification) =>
            for {
              _ <- repo.insert(notification)
              _ <- topic.publish1(notification)
            } yield committable.offset
          case Left(err) =>
            Logger[F].error(s"Decode error: ${err.getMessage}").as(committable.offset)
        }
      }
      .through(commitBatchWithin(100, 10.seconds))

}
