package com.example.auctionnotification.notification.listener;

import com.example.auctionnotification.notification.event.NotificationEvent;
import com.example.auctionnotification.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "auction-notification",
            groupId = "notification-group",
            containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consume(NotificationEvent event) {
        log.info("알림 이벤트 수신: type={}, receiverId={}", event.type(), event.receiverId());
        try {
            notificationService.save(event);
        } catch (Exception e) {
            log.error("알림 이벤트 처리 실패: type={}, receiverId={}", event.type(), event.receiverId());
            throw e;
        }
    }
}