// package services

// import java.time.Instant
// import java.util.UUID
// import models.IncomingKafkaEvent
// import models.Notification
// import models.NotificationEvent
// import models.NotificationEvent.*

// object NotificationMapper {

//   def fromEvent(event: IncomingKafkaEvent): Notification =
//     event match {
//       case IncomingKafkaEvent.QuestCreated(event) =>
//         Notification(
//           notificationId = UUID.randomUUID().toString,
//           userId = event.clientId,
//           title = event.title,
//           message = "Quest created with title Mikey",
//           eventType = "quest.completed",
//           createdAt = Instant.now(),
//           read = false
//         )
//       case IncomingKafkaEvent.QuestCompleted(event) =>
//         Notification(
//           notificationId = UUID.randomUUID().toString,
//           userId = event.clientId,
//           title = event.title,
//           message = s"You finished ${event.title}",
//           eventType = "quest.completed",
//           createdAt = Instant.now(),
//           read = false
//         )
//       case IncomingKafkaEvent.QuestUpdated(event) =>
//         Notification(
//           notificationId = UUID.randomUUID().toString,
//           userId = event.clientId,
//           title = event.title,
//           message = s"The quest '$questTitle' was updated.",
//           eventType = "quest.updated",
//           createdAt = Instant.now(),
//           read = false
//         )
//     }
// }
