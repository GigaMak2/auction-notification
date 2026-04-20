package com.example.auctionnotification.notification.repository;

import com.example.auctionnotification.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
