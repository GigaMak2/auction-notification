package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.enums.NotificationType;
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
    public void save(NotificationMessage message) {
        NotificationType type = message.type();
        String content = type.generateMessage(message);

        Notification notification = Notification.of(
                message.receiverId(),
                message.auctionId(),
                type,
                content
        );
        notificationRepository.save(notification);
        sseEmitterService.send(message.receiverId(), NotificationResponse.from(notification));
    }
}
