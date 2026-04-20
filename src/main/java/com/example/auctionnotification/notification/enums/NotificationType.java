package com.example.auctionnotification.notification.enums;

public enum NotificationType {
    AUCTION_STARTED,        // 경매 시작
    NEW_BID,                // 새 입찰 발생
    LOWEST_BID_UPDATED,     // 최저가 갱신
    AUCTION_CLOSED_WIN,     // 낙찰 (낙찰자)
    AUCTION_CLOSED_BUYER,   // 낙찰 (경매 생성자)
    AUCTION_NO_BID          // 유찰
}
