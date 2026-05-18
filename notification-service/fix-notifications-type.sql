USE flowboard_notification;

ALTER TABLE notifications
  MODIFY COLUMN type VARCHAR(32) NOT NULL;
