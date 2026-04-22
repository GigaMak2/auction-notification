package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.common.exception.ServiceErrorException;
import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.enums.NotificationType;
import com.example.auctionnotification.notification.exception.NotificationErrorEnum;
import com.example.auctionnotification.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

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

        Notification savedNotification = notificationRepository.save(notification);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseEmitterService.send(message.receiverId(), NotificationResponse.from(savedNotification));
            }
        });
    }

    @Transactional
    public void saveWithVT(NotificationMessage message) {
        NotificationType type = message.type();
        String content = type.generateMessage(message);

        Notification notification = Notification.of(
                message.receiverId(),
                message.auctionId(),
                type,
                content
        );

        Notification savedNotification = notificationRepository.save(notification);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseEmitterService.sendWithVT(message.receiverId(), NotificationResponse.from(savedNotification));
            }
        });
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
    public void markAsReadAll(Long userId) {
        List<Notification> notifications = notificationRepository.findAllByReceiverIdAndIsReadFalse(userId);

        for (Notification notification : notifications) {
            notification.markAsRead();
        }
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
