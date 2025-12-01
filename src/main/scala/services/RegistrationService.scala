package services

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Concurrent
import cats.implicits.*
import cats.syntax.all.*
import cats.Monad
import cats.NonEmptyParallel
import doobie.implicits.*
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.syntax.*
import java.time.Instant
import java.util.UUID
import kafka.*
import kafka.events.*
import kafka.events.UserRegisteredEvent
import models.database.*
import models.database.DatabaseErrors
import models.database.DatabaseSuccess
import models.outbox.OutboxEvent
import models.users.*
import models.UserType
import org.typelevel.log4cats.Logger
import repositories.OutboxRepositoryAlgebra
import repositories.UserDataRepositoryAlgebra

trait RegistrationServiceAlgebra[F[_]] {

  def getUser(userId: String): F[Option[UserData]]

  def registerUser(userId: String, registrationData: RegistrationData): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]]

  def deleteUser(userId: String): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]]
}

class RegistrationServiceImpl[F[_] : Concurrent : Monad : Logger](
  userRepo: UserDataRepositoryAlgebra[F],
  outboxRepo: OutboxRepositoryAlgebra[F]
) extends RegistrationServiceAlgebra[F] {


  override def getUser(userId: String): F[Option[UserData]] = 
    userRepo.findUser(userId)

  override def registerUser(userId: String, registrationData: RegistrationData): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]] = {

    val event =
      UserRegisteredEvent(
        userId = userId,
        username = registrationData.username,
        email = registrationData.email,
        userType = registrationData.userType,
        createdAt = Instant.now()
      )

    val outboxRecord =
      OutboxEvent.from(
        aggregateType = "User",
        aggregateId = userId,
        eventType = "UserRegisteredEvent",
        payload = event
      )

    // Atomic transaction: write user + outbox record
    val transaction: F[Validated[NonEmptyList[DatabaseErrors], DatabaseSuccess]] =
      for {
        dbResult <- userRepo.registerUser(userId, registrationData)
        _ <- dbResult match {
          case Valid(_) =>
            Logger[F].info(s"[RegistrationService][registerUser] - User $userId registered. Outbox event inserted.") *>
            outboxRepo.insert(outboxRecord).void
          case Invalid(_) =>
            Logger[F].info(s"[RegistrationService][registerUser] - Error Outbox event not inserted.") *>
            ().pure[F]
        }
      } yield dbResult

    transaction.flatTap {
      case Valid(_) =>
        Logger[F].info(s"[RegistrationService][registerUser] - User $userId registered. Outbox event saved.")
      case Invalid(e) =>
        Logger[F].warn(s"[RegistrationService][registerUser] - Failed registration for user $userId: ${e.toList.mkString(", ")}")
    }
  }

  override def deleteUser(userId: String): F[ValidatedNel[DatabaseErrors, DatabaseSuccess]] =
    userRepo.deleteUser(userId).flatTap {
      case Valid(_) =>
        Logger[F].info(s"[RegistrationService] Successfully deleted user $userId")
      case Invalid(errors) =>
        Logger[F].error(s"[RegistrationService] Failed to delete user $userId. Errors: ${errors.toList.mkString(", ")}")
    }
}

object RegistrationService {
  def apply[F[_] : Concurrent : Logger](
    userRepo: UserDataRepositoryAlgebra[F],
    outboxRepo: OutboxRepositoryAlgebra[F]
  ): RegistrationServiceAlgebra[F] =
    new RegistrationServiceImpl[F](
      userRepo,
      outboxRepo
    )
}
