package controllers

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.kernel.Async
import cats.effect.Concurrent
import cats.implicits.*
import fs2.Stream
import infrastructure.SessionCacheAlgebra
import io.circe.syntax.EncoderOps
import io.circe.Json
import models.database.UpdateSuccess
import models.responses.*
import models.users.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.syntax.all.http4sHeaderSyntax
import org.http4s.Challenge
import org.typelevel.log4cats.Logger
import scala.concurrent.duration.*
import services.RegistrationServiceAlgebra

trait RegistrationControllerAlgebra[F[_]] {
  def routes: HttpRoutes[F]
}

class RegistrationControllerImpl[F[_] : Async : Concurrent : Logger](
  registrationService: RegistrationServiceAlgebra[F],
  sessionCache: SessionCacheAlgebra[F]
) extends Http4sDsl[F]
    with RegistrationControllerAlgebra[F] {

  implicit val registrationDataDecoder: EntityDecoder[F, RegistrationData] = jsonOf[F, RegistrationData]
  implicit val updateUserDataDecoder: EntityDecoder[F, UpdateUserData] = jsonOf[F, UpdateUserData]
  implicit val createUserDataDecoder: EntityDecoder[F, CreateUserData] = jsonOf[F, CreateUserData]

  private def extractCookieSessionToken(req: Request[F]): Option[String] =
    req.cookies
      .find(_.name == "auth_session")
      .map(_.content)

  private def withValidSession(userId: String, token: String)(onValid: F[Response[F]]): F[Response[F]] =
    Logger[F].info(s"[RegistrationControllerImpl][withValidSession] UserId: $userId, token: $token") *>
      sessionCache.getSession(userId).flatMap {
        case Some(userSession) if userSession.cookieValue == token =>
          Logger[F].info(s"[RegistrationControllerImpl][withValidSession] User session: $userSession") *>
            onValid
        case Some(session) =>
          Logger[F].info(s"[RegistrationControllerImpl][withValidSession] User session does not match request user session token value from redis. $session") *>
            Forbidden("User session does not match request user session token value from redis.")
        case None =>
          Logger[F].info("[RegistrationControllerImpl][withValidSession] Invalid or expired session")
          Forbidden("Invalid or expired session")
      }

  private def withValidSessionCookieOnly(userId: String, token: String)(onValid: F[Response[F]]): F[Response[F]] =
    Logger[F].info(s"[RegistrationControllerImpl][withValidSession] UserId: $userId, token: $token") *>
      sessionCache.getSessionCookieOnly(userId).flatMap {
        case Some(cookieTokenValue) if cookieTokenValue == token =>
          Logger[F].info(s"[RegistrationControllerImpl][withValidSessionCookieOnly] CookieTokenValue: $cookieTokenValue") *>
            onValid
        case Some(session) =>
          Logger[F].info(s"[RegistrationControllerImpl][withValidSessionCookieOnly] User session does not match request user session token value from redis. $session") *>
            Forbidden("User session does not match request user session token value from redis.")
        case None =>
          Logger[F].info("[RegistrationControllerImpl][withValidSessionCookieOnly] Invalid or expired session")
          Forbidden("Invalid or expired session")
      }

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ GET -> Root / "registration" / "health" =>
      Logger[F].info(s"[BaseControllerImpl] GET - Health check for backend RegistrationController service") *>
        Ok(GetResponse("/devirl-auth-service/registration/health", "I am alive - RegistrationControllerImpl").asJson)

    case req @ GET -> Root / "registration" / "account" / "data" / userId =>
      (
        Logger[F].info(s"[RegistrationController][/user/data/$userId] GET - Attempting to retrieve user details") *>
          Async[F].pure(extractCookieSessionToken(req))
      ).flatMap {
        case Some(cookieToken) =>
          withValidSessionCookieOnly(userId, cookieToken) {
            Logger[F].info(s"[RegistrationController] GET - Authenticated for userId $userId") *>
              registrationService.getUser(userId).flatMap {
                case Some(user) =>
                  Logger[F].info(s"[RegistrationController] GET - Found user ${userId.toString()}") *>
                    Ok(user.asJson)
                case None =>
                  BadRequest(ErrorResponse("No_User_Data", "No user found").asJson)
              }
          }
        case None =>
          Logger[F].info(s"[RegistrationController] GET - Unauthorised") *>
            Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "api")), "Missing Cookie")
      }

    case req @ POST -> Root / "registration" / "account" / "data" / "create" / userId =>
      extractCookieSessionToken(req) match {
        case Some(cookieToken) =>
          withValidSessionCookieOnly(userId, cookieToken) {
            Logger[F].info(s"[RegistrationControllerImpl] POST - Registering user") *>
              req.decode[RegistrationData] { request =>
                registrationService.registerUser(userId, request).flatMap {
                  case Valid(response) =>
                    Logger[F].info(s"[RegistrationControllerImpl] POST - Successfully registered a user") *>
                      Created(CreatedResponse(response.toString, "user details successfully registered").asJson)
                  case Invalid(_) =>
                    InternalServerError(ErrorResponse(code = "Code", message = "An error occurred").asJson)
                }
              }
          }
        case None =>
          Logger[F].info(s"[RegistrationControllerImpl] POST - Successfully registered a user") *>
            Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "api")), "Missing Cookie")
      }
  }
}

object RegistrationController {

  def apply[F[_] : Async : Concurrent](
    registrationService: RegistrationServiceAlgebra[F],
    sessionCache: SessionCacheAlgebra[F]
  )(implicit logger: Logger[F]): RegistrationControllerAlgebra[F] =
    new RegistrationControllerImpl[F](registrationService, sessionCache)
}
