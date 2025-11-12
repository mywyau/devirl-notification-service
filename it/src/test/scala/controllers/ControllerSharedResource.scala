package controllers

import cats.effect.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import configuration.models.*
import configuration.AppConfig
import configuration.BaseAppConfig
import dev.profunktor.redis4cats.Redis
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import infrastructure.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import scala.concurrent.ExecutionContext
import test_routes.TestRoutes.*
import shared.HttpClientResource
import shared.SessionCacheResource
import shared.TransactorResource
import weaver.GlobalResource
import weaver.GlobalWrite

object ControllerSharedResource extends GlobalResource with BaseAppConfig {

  implicit val testLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def executionContextResource: Resource[IO, ExecutionContext] =
    ExecutionContexts.fixedThreadPool(4)

  def transactorResource(postgresqlConfig: PostgresqlConfig, ce: ExecutionContext): Resource[IO, HikariTransactor[IO]] =
    HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = s"jdbc:postgresql://${postgresqlConfig.host}:${postgresqlConfig.port}/${postgresqlConfig.dbName}",
      user = postgresqlConfig.username,
      pass = postgresqlConfig.password,
      connectEC = ce
    )

  def clientResource: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  def serverResource(
    appConfig: AppConfig,
    httpRoutes: HttpRoutes[IO]
  ) = {

    val hostResource: Resource[IO, Host] =
      Resource.eval(
        IO.fromEither(
          Host
            .fromString(appConfig.serverConfig.host)
            .toRight(new RuntimeException("[ControllerSharedResource] Invalid host configuration"))
        )
      )

    val portResource: Resource[IO, Port] =
      Resource.eval(
        IO.fromEither(
          Port
            .fromInt(appConfig.serverConfig.port)
            .toRight(new RuntimeException("[ControllerSharedResource] Invalid port configuration"))
        )
      )

    for {
      host: Host <- hostResource
      port: Port <- portResource
      router: HttpRoutes[IO] = httpRoutes
      server <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(router.orNotFound)
        .build
    } yield server
  }

  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      appConfig <- appConfigResource
      ce <- executionContextResource
      postgresqlConfig: PostgresqlConfig = appConfig.postgresqlConfig
      redisConfig: RedisConfig = appConfig.redisConfig
      xa <- transactorResource(postgresqlConfig, ce)
      sessionCache <- SessionCache.make[IO](appConfig)
      client <- clientResource
      testRouter <- createTestRouter(appConfig, xa)
      _ <- serverResource(appConfig, testRouter)
      _ <- global.putR(TransactorResource(xa))
      _ <- global.putR(HttpClientResource(client))
      _ <- global.putR(SessionCacheResource(sessionCache))
    } yield ()
}
