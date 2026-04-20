package com.example.auctionnotification.notification.listener;

import com.example.auctionnotification.notification.event.NotificationEvent;
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
        notificationService.save(event);
    }
}