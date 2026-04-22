package com.example.auctionnotification.notification.exception;

import com.example.auctionnotification.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorEnum implements ErrorEnumInterface {

    // 알림 관련
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 알림만 확인할 수 있습니다");

    private final HttpStatus status;
    private final String message;

    NotificationErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}