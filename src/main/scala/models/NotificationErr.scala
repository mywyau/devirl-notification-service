package models

sealed trait NotificationErr extends Product with Serializable
case object NotFound extends NotificationErr
case object Forbidden extends NotificationErr
