package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(300000L);
        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            try {
                previous.complete();
            } catch (Exception ignored) {}
        }

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }

        return emitter;
    }

    @Async
    public void send(Long userId, NotificationResponse response) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(response));
        } catch (IOException e) {
            log.error("SSE 전송 실패: userId={}", userId, e);
            emitters.remove(userId, emitter);
        }
    }

    @Async
    @Scheduled(fixedRate = 30000)
    public void sendPing() {
        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event().name("ping").data(""));
            } catch (IOException | IllegalStateException e) {
                log.warn("SSE ping 전송 실패 - emitter 제거: userId={}", userId, e);
                emitters.remove(userId, emitter);
            }
        }
    }
}
