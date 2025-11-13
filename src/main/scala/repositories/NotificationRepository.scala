package repositories

import cats.effect.MonadCancelThrow
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.TimestampMeta
import doobie.implicits.javatime.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import models.Notification

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

trait NotificationRepositoryAlgebra[F[_]] {

  def getByUserId(userId: String): F[List[Notification]]

  def getByUserIdPaged(userId: String, page: Int, pageSize: Int): F[List[Notification]]

  def getOwnerOf(notificationId: String): F[Option[String]]

  def markAsRead(notificationId: String): F[Int]
  
  def insert(n: Notification): F[Int]
}

class NotificationRepositoryImpl[F[_] : MonadCancelThrow](xa: Transactor[F]) extends NotificationRepositoryAlgebra[F] {

  // Map Java Instant to SQL TIMESTAMP
  implicit val instantMeta: Meta[Instant] =
    Meta[OffsetDateTime].imap(_.toInstant)(_.atOffset(ZoneOffset.UTC))

  override def getByUserId(userId: String): F[List[Notification]] =
    sql"""
      SELECT notification_id, user_id, title, message, event_type, created_at, read
      FROM notifications
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """
      .query[Notification]
      .to[List]
      .transact(xa)

  override def getByUserIdPaged(userId: String, page: Int, pageSize: Int): F[List[Notification]] = {
    val offset = page * pageSize
    sql"""
      SELECT notification_id, user_id, title, message, event_type, created_at, read
      FROM notifications
      WHERE user_id = $userId
      ORDER BY created_at DESC
      OFFSET $offset LIMIT $pageSize
    """
      .query[Notification]
      .to[List]
      .transact(xa)
  }

  override def getOwnerOf(notificationId: String): F[Option[String]] =
    sql"""
      SELECT user_id FROM notifications WHERE notification_id = $notificationId
    """
      .query[String]
      .option
      .transact(xa)

  override def markAsRead(notificationId: String): F[Int] =
    sql"""
      UPDATE notifications
      SET read = TRUE
      WHERE notification_id = $notificationId
    """.update.run
      .transact(xa)

  override def insert(n: Notification): F[Int] =
    sql"""
      INSERT INTO notifications (
        notification_id, user_id, title, message, event_type, created_at, read
      ) VALUES (
        ${n.notificationId}, ${n.userId}, ${n.title}, ${n.message}, ${n.eventType}, ${n.createdAt}, ${n.read}
      )
    """.update.run
      .transact(xa)
}

object NotificationRepository {
  def apply[F[_] : MonadCancelThrow](xa: Transactor[F]): NotificationRepositoryAlgebra[F] =
    new NotificationRepositoryImpl[F](xa)
}
