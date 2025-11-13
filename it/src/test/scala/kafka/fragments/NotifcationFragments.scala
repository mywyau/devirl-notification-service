package kafka.fragments

import doobie.implicits.*
import doobie.util.fragment.Fragment

object NotificationFragments {

  val resetNotificationTable: Fragment =
    sql"TRUNCATE TABLE notifications RESTART IDENTITY"

  val createNotificationTable: Fragment =
    sql"""
      CREATE TABLE IF NOT EXISTS notifications (
          id BIGSERIAL PRIMARY KEY,
          notification_id VARCHAR(255),
          user_id VARCHAR(255) NOT NULL,
          title VARCHAR(255) NOT NULL,
          message TEXT NOT NULL,
          event_type VARCHAR(255) NOT NULL,
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          read BOOLEAN NOT NULL DEFAULT FALSE
      );
    """
}
