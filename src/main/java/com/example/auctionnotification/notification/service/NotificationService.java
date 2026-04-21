package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.common.exception.ServiceErrorException;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.event.NotificationEvent;
import com.example.auctionnotification.notification.exception.NotificationErrorEnum;
import com.example.auctionnotification.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new ServiceErrorException(NotificationErrorEnum.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverId().equals(userId)) {
            throw new ServiceErrorException(NotificationErrorEnum.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Transactional
    public void delete(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new ServiceErrorException(NotificationErrorEnum.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverId().equals(userId)) {
            throw new ServiceErrorException(NotificationErrorEnum.NOTIFICATION_FORBIDDEN);
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteAllByReceiverId(userId);
    }
}
