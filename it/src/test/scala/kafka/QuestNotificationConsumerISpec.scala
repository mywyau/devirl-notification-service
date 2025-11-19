package kafka

import cats.effect.*
import cats.syntax.all.*
import consumers.QuestNotificationConsumer
import doobie.implicits.*
import doobie.util.transactor
import fs2.concurrent.Topic
import fs2.kafka.*
import io.circe.syntax.*
import kafka.fragments.NotificationFragments.*
import models.*
import models.NotificationEvent.QuestCompleted
import models.events.QuestCompletedEvent
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import repositories.NotificationRepositoryAlgebra
import repositories.NotificationRepositoryImpl
import shared.KafkaProducerResource
import shared.TransactorResource
import weaver.*

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.duration.*

import sys.process.*

class QuestNotificationConsumerISpec(global: GlobalRead) extends IOSuite {
  type Res = (KafkaProducerResource, TransactorResource)

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def sharedResource: Resource[IO, Res] =
    for {

      producer <- global.getOrFailR[KafkaProducerResource]()
      transactor <- global.getOrFailR[TransactorResource]()
      _ <- Resource.eval(
        createNotificationTable.update.run.transact(transactor.xa).void *>
          resetNotificationTable.update.run.transact(transactor.xa).void
      )
    } yield (producer, transactor)

  private def resetKafkaTopic(topic: String): IO[Unit] =
    IO.blocking {
      s"docker exec kafka-container-redpanda-1 rpk topic create $topic --brokers localhost:9092".!
    }.void

  private def deleteTopic(topic: String): IO[Unit] =
    IO.blocking {
      import sys.process.*
      s"docker exec kafka-container-redpanda-1 rpk topic delete $topic --brokers localhost:9092".!
    }.void

  test("QuestNotificationConsumer -  should consume NotificationEvent and insert Notification") { (sharedResource, log) =>

    val kafkaProducer = sharedResource._1
    val transactor = sharedResource._2

    val topicName = s"quest.events.test.v1"

    // Create a NotificationEvent (e.g. QuestCompletedEvent)
    val event =
      Envelope(
        typeName = "quest.completed",
        payload = QuestCompletedEvent(
          clientId = "test-user",
          questId = "quest001",
          title = "Quest Completed!",
          createdAt = Instant.now()
        ).asJson
      )

    val jsonEvent = event.asJson.noSpaces

    val repo = new NotificationRepositoryImpl(transactor.xa)

    for {
      _ <- resetKafkaTopic(topicName)

      topic <- Topic[IO, Notification]
      consumerSettings = ConsumerSettings[IO, String, String]
        .withBootstrapServers("localhost:9092")
        .withGroupId(s"notification-consumer-test")
        .withAutoOffsetReset(AutoOffsetReset.Earliest)

      consumer = new QuestNotificationConsumer[IO](topicName, topic, repo, consumerSettings)
      fiber <- consumer.stream.compile.drain.start
      _ <- IO.sleep(500.millis) // wait for consumer to subscribe
      // Produce a NotificationEvent to Kafka
      _ <- KafkaProducer
        .stream(ProducerSettings[IO, String, String].withBootstrapServers("localhost:9092"))
        .evalMap(_.produceOne_(topicName, "key", jsonEvent))
        .compile
        .drain

      // Wait for consumer to process it
      _ <- IO.sleep(3.seconds)

      // Verify that the repo got the new notification
      retrievedNotification <- repo.getByUserId("test-user")
      _ <- deleteTopic(topicName)

    } yield expect.all(
      retrievedNotification.nonEmpty,
      retrievedNotification.head.title == "Quest Completed!"
    )
  }
}
