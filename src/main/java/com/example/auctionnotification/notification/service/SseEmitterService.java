package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
        // 새 emitter 등록, 기존 emitter 있으면 연결 종료
        SseEmitter emitter = new SseEmitter(300000L);
        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            try {
                previous.complete();
            } catch (Exception ignored) {}
        }

        // 연결 종료 시 자신이 map에 등록된 경우에만 제거 (재구독 경합 방지)
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        // 더미 이벤트
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }

        return emitter;
    }

    @Async("notificationExecutor")
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

    @Async("notificationExecutorWithVT")
    public void sendWithVT(Long userId, NotificationResponse response) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(response));
        } catch (IOException e) {
            log.error("SSE 전송 실패(VT): userId={}", userId, e);
            emitters.remove(userId, emitter);
        }
    }
}
