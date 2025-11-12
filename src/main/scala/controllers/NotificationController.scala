package controllers

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.implicits.*
import fs2.Stream
import infrastructure.SessionCacheAlgebra
import io.circe.Json
import io.circe.syntax.EncoderOps
import models.*
import models.database.UpdateSuccess
import models.responses.*
import models.users.*
import org.http4s.*
import org.http4s.Challenge
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.syntax.all.http4sHeaderSyntax
import org.typelevel.log4cats.Logger
import services.NotificationServiceAlgebra

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*

trait NotificationControllerAlgebra[F[_]] {
  def routes: HttpRoutes[F]
}

class NotificationControllerImpl[F[_] : Async : Concurrent : Logger](
  notificationService: NotificationServiceAlgebra[F],
  sessionCache: SessionCacheAlgebra[F]
) extends Http4sDsl[F]
    with NotificationControllerAlgebra[F] {

  private def extractCookieSessionToken(req: Request[F]): Option[String] =
    req.cookies
      .find(_.name == "auth_session")
      .map(_.content)

  private def withValidSession(userId: String, token: String)(onValid: F[Response[F]]): F[Response[F]] =
    Logger[F].debug(s"[NotificationControllerImpl][withValidSession] UserId: $userId, token: $token") *>
      sessionCache.getSession(userId).flatMap {
        case Some(userSession) if userSession.cookieValue == token =>
          Logger[F].debug(s"[NotificationControllerImpl][withValidSession] User session: $userSession") *>
            onValid
        case Some(session) =>
          Logger[F].debug(s"[NotificationControllerImpl][withValidSession] User session does not match request user session token value from redis. $session") *>
            Forbidden("User session does not match request user session token value from redis.")
        case None =>
          Logger[F].debug("[NotificationControllerImpl][withValidSession] Invalid or expired session")
          Forbidden("Invalid or expired session")
      }

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ GET -> Root / "notification" / "health" =>
      Logger[F].debug(s"[BaseControllerImpl] GET - Health check for backend NotificationController service") *>
        Ok(GetResponse("/devirl-auth-service/Notification/health", "I am alive - NotificationControllerImpl").asJson)

    case req @ GET -> Root / "notifications" / userId =>
      val requesterId = userId // replace with actual auth lookup later
      notificationService.listForUser(requesterId, userId, page = 0, pageSize = 20).flatMap {
        case Right(list) => Ok(list.asJson)
        case Left(NotFound) => NotFound()
        case Left(Forbidden) => Forbidden()
        case Left(_) => InternalServerError()
      }
    // PATCH /notifications/:id/read
    case req @ PATCH -> Root / "notifications" / "read" / notifId =>
      val requesterId = "test-user" // get from auth
      notificationService.markAsRead(requesterId, notifId).flatMap {
        case Right(_) => NoContent()
        case Left(NotFound) => NotFound()
        case Left(Forbidden) => Forbidden()
        case Left(_) => InternalServerError()
      }

    /**
     * (Optional) POST /notifications/test — insert a dummy notification for
     * testing
     */
    // case POST -> Root / "notifications" / "test" =>
    //   val testNotification =
    //     Notification(
    //       id = UUID.randomUUID().toString,
    //       userId = "test-user",
    //       title = "Test Notification",
    //       message = "This is a test notification",
    //       eventType = "system",
    //       createdAt = Instant.now(),
    //       read = false
    //     )
    //   for {
    //     _ <- notificationService.insert(testNotification)
    //     resp <- Created(testNotification.asJson)
    //   } yield resp
  }
}

object NotificationController {

  def apply[F[_] : Async : Concurrent](
    notificationService: NotificationServiceAlgebra[F],
    sessionCache: SessionCacheAlgebra[F]
  )(implicit logger: Logger[F]): NotificationControllerAlgebra[F] =
    new NotificationControllerImpl[F](notificationService, sessionCache)
}
