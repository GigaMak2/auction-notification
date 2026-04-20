package com.example.auctionnotification.notification.repository;

import com.example.auctionnotification.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long userId);
}
