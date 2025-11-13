import cats.data.Kleisli
import cats.effect.*
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits.*
import cats.syntax.all.*
import cats.NonEmptyParallel
import com.comcast.ip4s.*
import configuration.AppConfig
import configuration.ConfigReader
import consumers.NotificationConsumer
import controllers.NotificationSocketRoutes
import dev.profunktor.redis4cats.RedisCommands
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fs2.concurrent.Topic
import fs2.kafka.AutoOffsetReset
import fs2.kafka.ConsumerSettings
import fs2.Stream
import infrastructure.KafkaProducerProvider
import java.time.Instant
import java.util.UUID
import middleware.Middleware.throttleMiddleware
import models.*
import modules.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Origin
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import org.http4s.server.Router
import org.http4s.server.Server
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.typelevel.ci.CIString
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import repositories.*
import routes.Routes.*
import scala.concurrent.duration.*
import scala.concurrent.duration.DurationInt
import services.*

object Main extends IOApp {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = {

    val serverResource: Resource[IO, org.http4s.server.Server] =
      for {
        config: AppConfig <- Resource.eval(ConfigReader[IO].loadAppConfig)
        transactor: HikariTransactor[IO] <- DatabaseModule.make[IO](config)
        redis: RedisCommands[IO, String, String] <- RedisModule.make[IO](config)
        // kafkaProducers: KafkaProducers[IO] <- KafkaModule.make[IO](config)
        httpClient: Client[IO] <- HttpClientModule.make[IO]
        httpApp: Kleisli[IO, Request[IO], Response[IO]] <- HttpModule.make(config, transactor)
        host: Host <- Resource.eval(IO.fromOption(Host.fromString(config.serverConfig.host))(new RuntimeException("Invalid host in configuration")))
        port: Port <- Resource.eval(IO.fromOption(Port.fromInt(config.serverConfig.port))(new RuntimeException("Invalid port in configuration")))

        notificationRepo = NotificationRepository[IO](transactor)

        //  Set up Kafka consumer configuration
        kafkaConsumerSettings =
          ConsumerSettings[IO, String, String]
            .withBootstrapServers(config.kafkaConfig.bootstrapServers)
            .withGroupId("notification-service")
            .withAutoOffsetReset(AutoOffsetReset.Earliest)

        initNotification =
          Notification(
            notificationId = UUID.randomUUID().toString,
            userId = "system",
            title = "init",
            message = "topic started",
            eventType = "system.init",
            createdAt = Instant.now(),
            read = true
          )

        // Create Topic inside Resource
        topic <- Resource.eval(Topic[IO, Notification])
        socketRoutes = new NotificationSocketRoutes[IO](topic)
        consumer = new NotificationConsumer[IO](topic, notificationRepo, kafkaConsumerSettings)
        _ <- Resource.eval(consumer.stream.compile.drain.start)

        // Combine REST + WebSocket routes
        server <- EmberServerBuilder
          .default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpWebSocketApp { wsBuilder =>
            val combinedApp: org.http4s.HttpApp[IO] = socketRoutes.routes(wsBuilder).orNotFound <+> httpApp
            combinedApp
          }
          .build

      } yield server

    // Keep server alive
    serverResource.use(_ => IO.never).as(ExitCode.Success)
  }
}
