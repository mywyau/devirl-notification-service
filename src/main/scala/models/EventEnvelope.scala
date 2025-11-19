package models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

sealed trait EventEnvelope

final case class Envelope[T](
  typeName: String,
  payload: T
) extends EventEnvelope

object Envelope {
  implicit def encoder[T: Encoder]: Encoder[Envelope[T]] = deriveEncoder
  implicit def decoder[T: Decoder]: Decoder[Envelope[T]] = deriveDecoder
}
