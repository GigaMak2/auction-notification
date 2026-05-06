package com.example.auctionnotification.notification;

import com.example.auctionnotification.notification.dto.NotificationMessage;
import com.example.auctionnotification.notification.entity.Notification;
import com.example.auctionnotification.notification.enums.NotificationType;
import com.example.auctionnotification.notification.repository.NotificationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret.key}")
    private String secretKey;

    private String userToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        userToken = generateToken(1L);
        otherToken = generateToken(2L);
    }

    @AfterEach
    void cleanUpRedis() {
        var factory = redisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }


    // ========================
    // 알림 목록 조회
    // ========================

    @Test
    @DisplayName("알림 목록 조회 성공")
    void getNotifications_success() throws Exception {
        // given
        saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);
        saveNotification(1L, 11L, NotificationType.NEW_BID);

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 알림 없음")
    void getNotifications_success_empty() throws Exception {
        // when & then
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("알림 목록 조회 실패 - 인증 없음")
    void getNotifications_fail_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }


    // ========================
    // 알림 읽음 처리
    // ========================

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_success() throws Exception {
        // given
        Long notificationId = saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);

        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("읽음 처리 요청 성공"));
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 본인 알림 아님")
    void markAsRead_fail_forbidden() throws Exception {
        // given
        Long notificationId = saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);

        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("본인 알림만 확인할 수 있습니다"));
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 알림 없음")
    void markAsRead_fail_notFound() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/notifications/999/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다"));
    }


    // ========================
    // 전체 읽음 처리
    // ========================

    @Test
    @DisplayName("전체 읽음 처리 성공")
    void markAsReadAll_success() throws Exception {
        // given
        saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);
        saveNotification(1L, 11L, NotificationType.NEW_BID);

        // when & then
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("전체 읽음 처리 요청 성공"));
    }


    // ========================
    // 알림 삭제
    // ========================

    @Test
    @DisplayName("알림 삭제 성공")
    void delete_success() throws Exception {
        // given
        Long notificationId = saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 삭제 요청 성공"));
    }

    @Test
    @DisplayName("알림 삭제 실패 - 본인 알림 아님")
    void delete_fail_forbidden() throws Exception {
        // given
        Long notificationId = saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("본인 알림만 확인할 수 있습니다"));
    }

    @Test
    @DisplayName("알림 삭제 실패 - 알림 없음")
    void delete_fail_notFound() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/notifications/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다"));
    }


    // ========================
    // 알림 전체 삭제
    // ========================

    @Test
    @DisplayName("알림 전체 삭제 성공")
    void deleteAll_success() throws Exception {
        // given
        saveNotification(1L, 10L, NotificationType.AUCTION_STARTED);
        saveNotification(1L, 11L, NotificationType.NEW_BID);

        // when & then
        mockMvc.perform(delete("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 전체 삭제 요청 성공"));
    }


    // ========================
    // 헬퍼 메서드
    // ========================

    private Long saveNotification(Long receiverId, Long auctionId, NotificationType type) {
        NotificationMessage msg = new NotificationMessage(type, receiverId, auctionId, "테스트 상품");
        Notification notification = Notification.of(receiverId, auctionId, type, type.generateMessage(msg));
        return notificationRepository.save(notification).getId();
    }

    private String generateToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.trim().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", "USER")
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 1800000))
                .signWith(key)
                .compact();
    }
}
