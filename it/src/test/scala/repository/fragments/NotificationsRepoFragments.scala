package repository.fragments

import doobie.implicits.*
import doobie.util.fragment

object NotificationsRepoFragments {

  val resetNotificationsTable: fragment.Fragment =
    sql"TRUNCATE TABLE notifications RESTART IDENTITY"

  val createNotificationsTable: fragment.Fragment =
    sql"""
      CREATE TABLE IF NOT EXISTS notifications (
        id BIGSERIAL PRIMARY KEY,
        notification_id VARCHAR(255) NOT NULL UNIQUE,
        client_id VARCHAR(255) NOT NULL,
        title VARCHAR(255) NOT NULL,
        message TEXT,
        event_type VARCHAR(100) NOT NULL,
        read BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    """

  val insertNotificationsData: fragment.Fragment =
    sql"""
        INSERT INTO notifications (
          notification_id,
          client_id,
          title,
          message,
          event_type,
          read,
          created_at,
          updated_at
        ) VALUES
          ('Notification001', 'Client001', 'some_notification_title_1', 'message_001', 'quest.event.v1', 'f', '2025-01-01 00:00:00', '2025-01-02 12:00:00'),
          ('Notification002', 'Client002', 'some_notification_title_2', 'message_002', 'quest.event.v1', 'f', '2025-01-01 00:00:00', '2025-01-02 12:00:00'),
          ('Notification003', 'Client003', 'some_notification_title_3', 'message_003', 'quest.event.v1', 'f', '2025-01-01 00:00:00', '2025-01-02 12:00:00')
    """
}
