package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
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
        // 유저가 SSE 구독 요청하면 emitter 만들어서 Map에 저장
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId, emitter);

        // 연결 끊기면 Map에서 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    public void send(Long userId, NotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(event));
        } catch (IOException e) {
            log.error("SSE 전송 실패: userId={}", userId);
            emitters.remove(userId);
        }
    }
}
