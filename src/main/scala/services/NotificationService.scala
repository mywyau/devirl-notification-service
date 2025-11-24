package services

import cats.Monad
import cats.NonEmptyParallel
import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.syntax.*
import kafka.*
import kafka.events.*
import models.*
import models.Forbidden
import models.NotFound
import models.Notification
import models.NotificationErr
import models.UserType
import models.database.DatabaseErrors
import models.database.DatabaseSuccess
import models.users.*
import org.typelevel.log4cats.Logger
import repositories.NotificationsRepositoryAlgebra

import java.time.Instant
import java.util.UUID

import models.database.*

trait NotificationServiceAlgebra[F[_]] {

  def listForUser(requesterId: String, userId: String, page: Int, pageSize: Int): F[Either[NotificationErr, List[Notification]]]

  def markAsRead(requesterId: String, notifId: String): F[Either[NotificationErr, Unit]]

  def createSystemNotification(targetUserId: String, title: String, message: String, eventType: String): F[Notification]
  // only internal callers use this
}

class NotificationServiceImpl[F[_] : Concurrent : Monad : Logger](
  repo: NotificationsRepositoryAlgebra[F]
) extends NotificationServiceAlgebra[F] {

  private def sanitize(s: String): String =
    s.trim.take(256)

  def listForUser(requesterId: String, userId: String, page: Int, pageSize: Int): F[Either[NotificationErr, List[Notification]]] =
    if (requesterId != userId) Forbidden.asLeft.pure[F]
    else {
      val p = math.max(page, 0)
      val ps = math.max(1, math.min(pageSize, 100))
      for {
        _ <- Logger[F].debug(s"[Notifications][list] user=$userId page=$p size=$ps")
        rows <- repo.getByUserIdPaged(userId, p, ps)
      } yield rows.asRight
    }

  def markAsRead(requesterId: String, notifId: String): F[Either[NotificationErr, Unit]] =
    for {
      ownerOpt <- repo.getOwnerOf(notifId) // returns Option[userId]
      res <- ownerOpt match {
        case None =>
          NotFound.asLeft.pure[F]
        case Some(owner) if owner != requesterId =>
          Forbidden.asLeft.pure[F]
        case Some(_) =>
          repo.markAsRead(notifId).map(rows => if (rows > 0) ().asRight else NotFound.asLeft)
      }
    } yield res

  def createSystemNotification(targetUserId: String, title: String, message: String, eventType: String): F[Notification] = {
    val n = Notification(
      notificationId = UUID.randomUUID().toString,
      userId = targetUserId,
      title = sanitize(title),
      message = message.take(2000),
      eventType = eventType,
      createdAt = Instant.now(),
      read = false
    )
    repo.insert(n).as(n)
  }
}

object NotificationService {
  def apply[F[_] : Concurrent : Logger](repo: NotificationsRepositoryAlgebra[F]): NotificationServiceAlgebra[F] =
    new NotificationServiceImpl[F](repo)
}
