package repository

import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.Resource
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import java.time.Instant
import java.time.LocalDateTime
import models.*
import models.database.*
import models.database.DeleteSuccess
import models.database.UpdateSuccess
import models.Completed
import models.Demonic
import models.InProgress
import repositories.NotificationsRepository.*
import repositories.NotificationsRepositoryImpl
import repository.fragments.NotificationsRepoFragments.*
import repository.RepositoryISpecBase
import scala.collection.immutable.ArraySeq
import shared.TransactorResource
import test_data.ITestConstants.*
import weaver.GlobalRead
import weaver.IOSuite
import weaver.ResourceTag

class NotificationsRepositoryISpec(global: GlobalRead) extends IOSuite with RepositoryISpecBase {

  type Res = NotificationsRepositoryImpl[IO]

  private def initializeSchema(transactor: TransactorResource): Resource[IO, Unit] =
    Resource.eval(
      createNotificationsTable.update.run.transact(transactor.xa).void *>
        resetNotificationsTable.update.run.transact(transactor.xa).void *>
        insertNotificationsData.update.run.transact(transactor.xa).void
    )

//   def testNotificationsRequest(clientId: String, businessId: String): Notification =
//     Notification(
//       rank = Demonic,
//       title = "Implement User Authentication",
//       description = Some("Set up Auth0 integration and secure routes using JWT tokens."),
//       acceptanceCriteria = "Set up Auth0 integration and secure routes using JWT tokens.",
//       tags = Seq(Python, Scala, TypeScript)
//     )

  def sharedResource: Resource[IO, NotificationsRepositoryImpl[IO]] = {
    val setup = for {
      transactor <- global.getOrFailR[TransactorResource]()
      notificationsRepo = new NotificationsRepositoryImpl[IO](transactor.xa)
      createSchemaIfNotPresent <- initializeSchema(transactor)
    } yield notificationsRepo

    setup
  }

  test(".findAllByUserId() - should find and return the notifications if user_id exists for a previously created notifications") { notificationsRepo =>

    val notificationId = "Notifcation001"
    val userId = "USER001"

    val expectedResult =
      Notification(
        notificationId = notificationId,
        userId = "client001",
        title = "some_notifications_title",
        message = "some_notifications_message",
        eventType = "quest.events.v1",
        read = false,
        createdAt = Instant.now()
      )

    for {
      notificationss <- notificationsRepo.getByUserId(userId)
    } yield expect(notificationss == List(expectedResult))
  }

//   test(".findByNotificationsId() - should find and return the notifications if notifications_id exists for a previously created notifications") { notificationsRepo =>

//     val notificationId = "Notifcation003"

//     val expectedResult =
//       Notification(
//         notificationId = notificationId,
//         userId = "client001",
//         title = "some_notifications_title",
//         message = "some_notifications_message",
//         eventType = "quest.events.v1",
//         read = false,
//         createdAt = Instant.now()
//       )

//     for {
//       notificationsOpt <- notificationsRepo.findByNotificationsId(notificationId)
//     } yield expect(notificationsOpt == Some(expectedResult))
//   }

//   test(".update() - for a given notifications_id should update the notifications details if previously created notifications exists") { notificationsRepo =>

//     val updateRenotifications =
//       UpdateNotification(
//         rank = Demonic,
//         title = "Implement User Authentication",
//         description = Some("Set up Auth0 integration and secure routes using JWT tokens."),
//         acceptanceCriteria = Some("Set up Auth0 integration and secure routes using JWT tokens.")
//       )

//     for {
//       notificationsOpt <- notificationsRepo.update("QUEST002", updateRenotifications)
//     } yield expect(notificationsOpt == Valid(UpdateSuccess))
//   }

//   test(".deleteNotifications() - should delete the Notifcation003 notification if notifications_id exists for the previously existing notification") { notificationsRepo =>

//     val notificationId = "Notifcation003"

//     val expectedResult =
//       Notification(
//         notificationId = notificationId,
//         userId = "client001",
//         title = "some_notifications_title",
//         message = "some_notifications_message",
//         eventType = "quest.events.v1",
//         read = false,
//         createdAt = Instant.now()
//       )

//     for {
//       firstFindResult <- notificationsRepo.findByNotificationsId(notificationsId)
//       deleteResult <- notificationsRepo.delete(notificationsId)
//       afterDeletionFindResult <- notificationsRepo.findByNotificationsId(notificationsId)
//     } yield expect.all(
//       firstFindResult == Some(expectedResult),
//       deleteResult == Valid(DeleteSuccess),
//       afterDeletionFindResult == None
//     )
//   }
}
