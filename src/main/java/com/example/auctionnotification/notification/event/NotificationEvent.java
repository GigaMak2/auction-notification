package com.example.auctionnotification.notification.event;

import com.example.auctionnotification.notification.enums.NotificationType;

public record NotificationEvent(
        NotificationType type,
        Long receiverId,
        Long auctionId,
        String message
) {}
