package routes

import cats.effect.*
import cats.NonEmptyParallel
import configuration.AppConfig
import controllers.*
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import infrastructure.*
import java.net.URI
import kafka.*
import org.http4s.client.Client
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import repositories.*
import services.*

object Routes {

  def baseRoutes[F[_] : Concurrent : Logger](): HttpRoutes[F] = {

    val baseController = BaseController()

    baseController.routes
  }

  def notificationRoutes[F[_] : Async: Concurrent : Logger](
    appConfig: AppConfig,
    transactor: Transactor[F]
  ): HttpRoutes[F] = {

    val sessionCache = SessionCache(appConfig)
    val notificationRepository = NotificationRepository(transactor)
    val notificationService = NotificationService(notificationRepository)
    val notificationController = NotificationController(sessionCache, notificationService)

    notificationController.routes
  }

}
