package com.example.auctionnotification.notification.controller;

import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.enums.NotificationType;
import com.example.auctionnotification.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/notifications")
public class NotificationTestController {

    private final NotificationService notificationService;

    @PostMapping("/trigger")
    public ResponseEntity<Void> trigger(@RequestParam(defaultValue = "1") Long userId) {
        NotificationMessage message = new NotificationMessage(
                NotificationType.NEW_BID,
                userId,
                1L,
                "테스트 경매 상품"
        );
        notificationService.save(message);
        return ResponseEntity.ok().build();
    }
}
