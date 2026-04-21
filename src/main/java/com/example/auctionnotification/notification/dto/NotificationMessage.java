package com.example.auctionnotification.notification.dto;

import com.example.auctionnotification.notification.enums.NotificationType;

import java.util.Objects;

public record NotificationMessage(
        NotificationType type,
        Long receiverId,
        Long auctionId,
        String itemName
) {
    public NotificationMessage {
        Objects.requireNonNull(type, "알림 타입은 필수입니다");
        Objects.requireNonNull(receiverId, "수신자 ID는 필수입니다");
        Objects.requireNonNull(auctionId, "경매 ID는 필수입니다");
        Objects.requireNonNull(itemName, "경매 상품명은 필수입니다");
    }
}
