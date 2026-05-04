package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @InjectMocks
    private SseEmitterService sseEmitterService;

    private Map<Long, SseEmitter> emitters;

    @BeforeEach
    void setUp() {
        emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(sseEmitterService, "emitters");
    }


    // ========================
    // 구독
    // ========================

    @Test
    @DisplayName("구독 성공 - 새 emitter 등록")
    void subscribe_success() {
        // when
        SseEmitter emitter = sseEmitterService.subscribe(1L);

        // then
        assertThat(emitter).isNotNull();
        assertThat(emitters).containsKey(1L);
        assertThat(emitters.get(1L)).isSameAs(emitter);
    }

    @Test
    @DisplayName("구독 성공 - 기존 emitter 있으면 complete 후 교체")
    void subscribe_success_replacePreviousEmitter() {
        // given
        SseEmitter previous = mock(SseEmitter.class);
        emitters.put(1L, previous);

        // when
        SseEmitter newEmitter = sseEmitterService.subscribe(1L);

        // then
        verify(previous).complete();
        assertThat(emitters.get(1L)).isSameAs(newEmitter);
        assertThat(emitters.get(1L)).isNotSameAs(previous);
    }

    @Test
    @DisplayName("구독 성공 - 기존 emitter complete 실패해도 교체 진행")
    void subscribe_success_previousEmitterCompleteFails() {
        // given
        SseEmitter previous = mock(SseEmitter.class);
        doThrow(new IllegalStateException("already completed")).when(previous).complete();
        emitters.put(1L, previous);

        // when
        SseEmitter newEmitter = sseEmitterService.subscribe(1L);

        // then (예외 없이 정상 교체)
        assertThat(emitters.get(1L)).isSameAs(newEmitter);
    }


    // ========================
    // 알림 전송
    // ========================

    @Test
    @DisplayName("알림 전송 성공")
    void send_success() throws IOException {
        // given
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(1L, emitter);

        NotificationResponse response = new NotificationResponse(
                1L, 10L, NotificationType.AUCTION_STARTED, "'상품A' 경매가 시작됐습니다.", false, LocalDateTime.now());

        // when
        sseEmitterService.send(1L, response);

        // then
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("알림 전송 스킵 - emitter 없음")
    void send_skip_noEmitter() {
        // given
        NotificationResponse response = new NotificationResponse(
                1L, 10L, NotificationType.AUCTION_STARTED, "'상품A' 경매가 시작됐습니다.", false, LocalDateTime.now());

        // when
        sseEmitterService.send(1L, response);

        // then (예외 없이 정상 종료)
        assertThat(emitters).doesNotContainKey(1L);
    }

    @Test
    @DisplayName("알림 전송 실패 - IOException 발생 시 emitter 제거")
    void send_fail_ioException() throws IOException {
        // given
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(1L, emitter);
        doThrow(new IOException("연결 끊김")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        NotificationResponse response = new NotificationResponse(
                1L, 10L, NotificationType.AUCTION_STARTED, "'상품A' 경매가 시작됐습니다.", false, LocalDateTime.now());

        // when
        sseEmitterService.send(1L, response);

        // then
        assertThat(emitters).doesNotContainKey(1L);
    }


    // ========================
    // 핑 전송
    // ========================

    @Test
    @DisplayName("핑 전송 성공 - 모든 emitter에 ping 전송")
    void sendPing_success() throws IOException {
        // given
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        emitters.put(1L, emitter1);
        emitters.put(2L, emitter2);

        // when
        sseEmitterService.sendPing();

        // then
        verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("핑 전송 실패 - IOException 발생 시 해당 emitter만 제거")
    void sendPing_fail_ioException() throws IOException {
        // given
        SseEmitter failEmitter = mock(SseEmitter.class);
        SseEmitter okEmitter = mock(SseEmitter.class);
        emitters.put(1L, failEmitter);
        emitters.put(2L, okEmitter);
        doThrow(new IOException("연결 끊김")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        sseEmitterService.sendPing();

        // then
        assertThat(emitters).doesNotContainKey(1L);
        assertThat(emitters).containsKey(2L);
    }

    @Test
    @DisplayName("핑 전송 실패 - IllegalStateException 발생 시 해당 emitter만 제거")
    void sendPing_fail_illegalStateException() throws IOException {
        // given
        SseEmitter failEmitter = mock(SseEmitter.class);
        SseEmitter okEmitter = mock(SseEmitter.class);
        emitters.put(1L, failEmitter);
        emitters.put(2L, okEmitter);
        doThrow(new IllegalStateException("emitter closed")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        sseEmitterService.sendPing();

        // then
        assertThat(emitters).doesNotContainKey(1L);
        assertThat(emitters).containsKey(2L);
    }

    @Test
    @DisplayName("핑 전송 - emitter 없으면 아무것도 안 함")
    void sendPing_noEmitters() {
        // when
        sseEmitterService.sendPing();

        // then
        assertThat(emitters).isEmpty();
    }
}