package com.example.auctionnotification.notification.service;

import com.example.auctionnotification.common.exception.ServiceErrorException;
import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.enums.NotificationType;
import com.example.auctionnotification.notification.exception.NotificationErrorEnum;
import com.example.auctionnotification.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    private MockedStatic<TransactionSynchronizationManager> mockedTsm;

    @BeforeEach
    void setUpTransactionSync() {
        mockedTsm = mockStatic(TransactionSynchronizationManager.class);
    }

    @AfterEach
    void tearDownTransactionSync() {
        mockedTsm.close();
    }


    // ========================
    // 알림 저장
    // ========================

    @Test
    @DisplayName("알림 저장 성공")
    void save_success() {
        // given
        NotificationMessage message = new NotificationMessage(
                NotificationType.AUCTION_STARTED, 1L, 10L, "테스트 상품");

        Notification notification = Notification.of(1L, 10L, NotificationType.AUCTION_STARTED,
                "'테스트 상품' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(notification, "id", 1L);

        given(notificationRepository.save(any(Notification.class))).willReturn(notification);

        // when
        notificationService.save(message);

        // then
        verify(notificationRepository).save(any(Notification.class));
        mockedTsm.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
    }

    @Test
    @DisplayName("알림 저장 성공 - 모든 NotificationType")
    void save_success_allTypes() {
        // given
        for (NotificationType type : NotificationType.values()) {
            NotificationMessage message = new NotificationMessage(type, 1L, 10L, "테스트 상품");

            Notification notification = Notification.of(1L, 10L, type, type.generateMessage(message));
            ReflectionTestUtils.setField(notification, "id", 1L);

            given(notificationRepository.save(any(Notification.class))).willReturn(notification);

            // when
            notificationService.save(message);
        }

        // then
        verify(notificationRepository, times(NotificationType.values().length)).save(any(Notification.class));
    }


    // ========================
    // 알림 목록 조회
    // ========================

    @Test
    @DisplayName("알림 목록 조회 성공")
    void getNotifications_success() {
        // given
        Long userId = 1L;

        Notification n1 = Notification.of(userId, 10L, NotificationType.AUCTION_STARTED, "'상품A' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(n1, "id", 1L);

        Notification n2 = Notification.of(userId, 11L, NotificationType.NEW_BID, "'상품B' 경매에 새 입찰이 들어왔습니다.");
        ReflectionTestUtils.setField(n2, "id", 2L);

        given(notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(n1, n2));

        // when
        List<NotificationResponse> responses = notificationService.getNotifications(userId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().notificationId()).isEqualTo(1L);
        assertThat(responses.getFirst().type()).isEqualTo(NotificationType.AUCTION_STARTED);
        assertThat(responses.getFirst().isRead()).isFalse();
        assertThat(responses.get(1).notificationId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 알림 없음")
    void getNotifications_success_empty() {
        // given
        Long userId = 1L;
        given(notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId)).willReturn(List.of());

        // when
        List<NotificationResponse> responses = notificationService.getNotifications(userId);

        // then
        assertThat(responses).isEmpty();
    }


    // ========================
    // 알림 읽음 처리
    // ========================

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_success() {
        // given
        Long userId = 1L;
        Long notificationId = 1L;

        Notification notification = Notification.of(userId, 10L, NotificationType.AUCTION_STARTED,
                "'상품A' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(notificationId, userId);

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 알림 없음")
    void markAsRead_fail_notFound() {
        // given
        Long userId = 1L;
        Long notificationId = 1L;

        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(NotificationErrorEnum.NOTIFICATION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 본인 알림 아님")
    void markAsRead_fail_forbidden() {
        // given
        Long userId = 99L;
        Long notificationId = 1L;

        Notification notification = Notification.of(1L, 10L, NotificationType.AUCTION_STARTED,
                "'상품A' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(NotificationErrorEnum.NOTIFICATION_FORBIDDEN.getMessage());
    }


    // ========================
    // 전체 읽음 처리
    // ========================

    @Test
    @DisplayName("전체 읽음 처리 성공")
    void markAsReadAll_success() {
        // given
        Long userId = 1L;

        Notification n1 = Notification.of(userId, 10L, NotificationType.AUCTION_STARTED, "'상품A' 경매가 시작됐습니다.");
        Notification n2 = Notification.of(userId, 11L, NotificationType.NEW_BID, "'상품B' 경매에 새 입찰이 들어왔습니다.");

        given(notificationRepository.findAllByReceiverIdAndIsReadFalse(userId)).willReturn(List.of(n1, n2));

        // when
        notificationService.markAsReadAll(userId);

        // then
        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
    }

    @Test
    @DisplayName("전체 읽음 처리 성공 - 읽지 않은 알림 없음")
    void markAsReadAll_success_noUnread() {
        // given
        Long userId = 1L;
        given(notificationRepository.findAllByReceiverIdAndIsReadFalse(userId)).willReturn(List.of());

        // when
        notificationService.markAsReadAll(userId);

        // then (예외 없이 정상 종료)
        verify(notificationRepository).findAllByReceiverIdAndIsReadFalse(userId);
    }


    // ========================
    // 알림 삭제
    // ========================

    @Test
    @DisplayName("알림 삭제 성공")
    void delete_success() {
        // given
        Long userId = 1L;
        Long notificationId = 1L;

        Notification notification = Notification.of(userId, 10L, NotificationType.AUCTION_STARTED,
                "'상품A' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.delete(notificationId, userId);

        // then
        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("알림 삭제 실패 - 알림 없음")
    void delete_fail_notFound() {
        // given
        Long userId = 1L;
        Long notificationId = 1L;

        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.delete(notificationId, userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(NotificationErrorEnum.NOTIFICATION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("알림 삭제 실패 - 본인 알림 아님")
    void delete_fail_forbidden() {
        // given
        Long userId = 99L;
        Long notificationId = 1L;

        Notification notification = Notification.of(1L, 10L, NotificationType.AUCTION_STARTED,
                "'상품A' 경매가 시작됐습니다.");
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.delete(notificationId, userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(NotificationErrorEnum.NOTIFICATION_FORBIDDEN.getMessage());
    }


    // ========================
    // 알림 전체 삭제
    // ========================

    @Test
    @DisplayName("알림 전체 삭제 성공")
    void deleteAll_success() {
        // given
        Long userId = 1L;

        // when
        notificationService.deleteAll(userId);

        // then
        verify(notificationRepository).deleteAllByReceiverId(userId);
    }
}