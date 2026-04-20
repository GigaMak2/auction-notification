package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.event.NotificationEvent;
import com.example.auctionnotification.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public void save(NotificationEvent event) {
        Notification notification = Notification.of(
                event.receiverId(),
                event.auctionId(),
                event.type(),
                event.message()
        );
        notificationRepository.save(notification);
        sseEmitterService.send(event.receiverId(), event);
    }
}
