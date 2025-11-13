package controllers

import cats.effect.*
import cats.implicits.*
import fs2.concurrent.Topic
import fs2.Pipe
import fs2.Stream
import io.circe.syntax.* // this needs to be above fs2 import or not import fs2.*
import java.time.Instant
import java.util.UUID
import models.Notification
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.HttpRoutes
import scala.concurrent.duration.*

class NotificationSocketRoutes[F[_] : Async](
  topic: Topic[F, Notification]
) extends Http4sDsl[F] {

  // Helper: convert Notification → WebSocket JSON message
  private def toJsonFrame(n: Notification): WebSocketFrame.Text =
    WebSocketFrame.Text(n.asJson.noSpaces)

  // Dummy heartbeat messages (until Kafka integration)
  private val dummyStream: Stream[F, Notification] =
    Stream.awakeEvery[F](3.seconds).map { _ =>
      Notification(
        notificationId = UUID.randomUUID().toString,
        userId = "test-user",
        title = "Dummy Notification",
        message = "You have a new dummy event!",
        eventType = "system.dummy",
        createdAt = Instant.now(),
        read = false
      )
    }

  def routes(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "ws" / userId =>
      val send: Stream[F, WebSocketFrame] =
        topic
          .subscribe(1000) // replay last 1000 messages
          .filter(_.userId == userId)
          .merge(dummyStream) // merge dummy notifications for testing
          .map(toJsonFrame)

      val receive: Pipe[F, WebSocketFrame, Unit] =
        _.evalMap {
          case WebSocketFrame.Text(msg, _) =>
            Async[F].delay(println(s"[WS:$userId] received: $msg"))
          case _ => Async[F].unit
        }

      builder.build(send, receive)
    }
}
