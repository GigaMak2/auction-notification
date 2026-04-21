package com.example.auctionnotification.notification.dto;

import com.example.auctionnotification.notification.enums.NotificationType;

public record NotificationMessage(
        NotificationType type,
        Long receiverId,
        Long auctionId,
        String itemName
) {}
