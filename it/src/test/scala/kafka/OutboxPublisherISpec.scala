package kafka

import cats.effect.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.util.fragment
import fs2.kafka.*
import io.circe.syntax.*
import java.time.Instant
import kafka.events.*
import kafka.events.UserRegisteredEvent
import kafka.fragments.OutboxSqlFragments.*
import models.outbox.OutboxEvent
import models.Dev
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import repositories.*
import scala.concurrent.duration.*
import services.OutboxPublisherService
import services.OutboxPublisherServiceImpl
import shared.*
import weaver.*

class OutboxPublisherISpec(global: GlobalRead) extends IOSuite {
  type Res = (KafkaProducerResource, OutboxRepositoryImpl[IO])

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def sharedResource: Resource[IO, Res] =
    for {
      transactor <- global.getOrFailR[TransactorResource]()
      _ <- Resource.eval(
        createOutboxTable.update.run.transact(transactor.xa).void *>
          resetOutboxTable.update.run.transact(transactor.xa).void
      )
      producer <- global.getOrFailR[KafkaProducerResource]()
      outboxRepo = new OutboxRepositoryImpl[IO](transactor.xa)
    } yield (producer, outboxRepo)

  private def resetKafkaTopic(topic: String): IO[Unit] =
    IO.blocking {
      import sys.process._
      s"docker exec kafka-container-redpanda-1 rpk topic create $topic --brokers localhost:9092".!
    }.void

  private def deleteTopic(topic: String): IO[Unit] =
    IO.blocking {
      import sys.process._
      s"docker exec kafka-container-redpanda-1 rpk topic delete $topic --brokers localhost:9092".!
    }.void

  test("OutboxPublisher - publishes UNPUBLISHED events from Outbox table and marks them as published") { (sharedResource, log) =>

    val kafkaRes = sharedResource._1
    val outboxRepo = sharedResource._2

    val topic = "user.registered.test"

    val event =
      UserRegisteredEvent(
        userId = "user-123",
        username = "Alice",
        email = "alice@example.com",
        userType = Dev,
        createdAt = Instant.parse("2025-11-09T20:00:00Z")
      )

    val outbox =
      OutboxEvent
        .from(
          aggregateType = "User",
          aggregateId = event.userId,
          eventType = "UserRegisteredEvent",
          payload = event
        )

    val registrationProducer =
      new RegistrationEventProducerImpl[IO](topic, kafkaRes.producer)

    val outBoxPublisher =
      OutboxPublisherService[IO](
        outboxRepo = outboxRepo,
        registrationEventProducer = registrationProducer,
        topicName = topic
      )

    // Helper: read 1 message from a topic (with a fresh consumer group)
    def readTopicMessages(topicName: String): IO[List[String]] = {
      val consumerSettings =
        ConsumerSettings[IO, String, String]
          .withBootstrapServers("localhost:9092") // or use your resource value
          .withGroupId(s"outbox-it-test-consumer}")
          .withAutoOffsetReset(AutoOffsetReset.Earliest)

      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topicName)
        .records
        .map(_.record.value)
        .take(1) // read at least one published record
        .compile
        .toList
        .timeout(5.seconds)
    }

    for {
      _ <- resetKafkaTopic(topic)
      _ <- outboxRepo.insert(outbox) // step 1: simulate a pending outbox record
      fiber <- outBoxPublisher.stream.compile.drain.start // step 2: start the publisher
      _ <- IO.sleep(2.seconds) // give it time to pick up the event
      publishedEvents <- readTopicMessages(topic) // step 3: consume from Kafka
      decodedEvents = publishedEvents.flatMap { jsonStr =>
        io.circe.parser.decode[UserRegisteredEvent](jsonStr).toOption
      }
      _ <- logger.info(s"[OutboxPublisherISpec][test-1] $decodedEvents")
      dbState <- outboxRepo.fetchUnpublished(10) // step 4: check DB state
      _ <- fiber.cancel
      _ <- deleteTopic(topic)
    } yield expect.all(
      publishedEvents.nonEmpty,
      decodedEvents == List(event),
      dbState.isEmpty
    )
  }
}
