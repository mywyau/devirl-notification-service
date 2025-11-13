package kafka

import cats.effect.*
import cats.syntax.all.*
import consumers.NotificationConsumer
import doobie.implicits.*
import doobie.util.transactor
import fs2.concurrent.Topic
import fs2.kafka.*
import io.circe.syntax.*
import java.time.Instant
import kafka.fragments.NotificationFragments.*
import models.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import repositories.NotificationRepositoryAlgebra
import repositories.NotificationRepositoryImpl
import scala.collection.mutable
import scala.concurrent.duration.*
import shared.KafkaProducerResource
import shared.TransactorResource
import sys.process.*
import weaver.*
import models.NotificationEvent.QuestCompleted

class NotificationKafkaEndToEndISpec(global: GlobalRead) extends IOSuite {
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

  test("NotificationConsumer should consume NotificationEvent and insert Notification") { (sharedResource, log) =>

    val kafkaProducer = sharedResource._1
    val transactor = sharedResource._2

    val topicName = s"notifications"

    // Create a NotificationEvent (e.g. quest completed)
    val event =
      QuestCompleted(
        userId = "test-user",
        questTitle = "Implement NotificationConsumer",
        reward = 250
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

      // Real consumer using your NotificationConsumer class
      consumer = new NotificationConsumer[IO](topic, repo, consumerSettings)

      // Start consuming in the background
      fiber <- consumer.stream.compile.drain.start

      _ <- IO.sleep(500.millis) // wait for consumer to subscribe

      // Produce a NotificationEvent to Kafka
      _ <- KafkaProducer
        .stream(ProducerSettings[IO, String, String].withBootstrapServers("localhost:9092"))
        .evalMap(_.produceOne_("notifications", "key", jsonEvent))
        .compile
        .drain

      // Wait for consumer to process it
      _ <- IO.sleep(3.seconds)

      // Verify that the repo got the new notification
      retrievedNotification <- repo.getByUserId("test-user")
      _ <- deleteTopic(topicName)

    } yield expect(retrievedNotification.nonEmpty && retrievedNotification.head.title == "Quest Completed!")

  }
}