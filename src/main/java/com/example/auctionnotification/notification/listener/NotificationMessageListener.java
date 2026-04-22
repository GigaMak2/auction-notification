package com.example.auctionnotification.notification.listener;

import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public void onMessage(Message message, byte [] pattern) {
        try {
            NotificationMessage payload = objectMapper.readValue(
                    message.getBody(),
                    NotificationMessage.class
            );
            String channel = new String(pattern);
            if (channel.equals("auction:notification:vt")) {
                notificationService.saveWithVT(payload);
            } else  {
                notificationService.save(payload);
            }
        } catch (Exception e) {
            log.error("알림 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }
}