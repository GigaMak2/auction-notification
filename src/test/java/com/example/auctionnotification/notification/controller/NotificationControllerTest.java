package com.example.auctionnotification.notification.controller;

import com.example.auctionnotification.common.config.security.CustomUserDetails;
import com.example.auctionnotification.common.exception.GlobalExceptionHandler;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.enums.NotificationType;
import com.example.auctionnotification.notification.service.NotificationService;
import com.example.auctionnotification.notification.service.SseEmitterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({NotificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUpSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========================
    // SSE 구독
    // ========================

    @Test
    @DisplayName("SSE 구독 성공")
    void subscribe_success() throws Exception {
        // given
        given(sseEmitterService.subscribe(1L)).willReturn(new SseEmitter());

        // when & then
        mockMvc.perform(get("/api/notifications/subscribe")
                        .header("Authorization", "Bearer accessToken")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(request().asyncStarted())
                .andDo(document("notification/subscribe",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));

        verify(sseEmitterService).subscribe(1L);
    }


    // ========================
    // 알림 목록 조회
    // ========================

    @Test
    @DisplayName("알림 목록 조회 성공")
    void getNotifications_success() throws Exception {
        // given
        List<NotificationResponse> responses = List.of(
                new NotificationResponse(1L, 10L, NotificationType.AUCTION_STARTED,
                        "'테스트 상품' 경매가 시작됐습니다.", false, LocalDateTime.now()),
                new NotificationResponse(2L, 11L, NotificationType.NEW_BID,
                        "'테스트 상품' 경매에 새 입찰이 들어왔습니다.", true, LocalDateTime.now())
        );

        given(notificationService.getNotifications(1L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].notificationId").value(1L))
                .andExpect(jsonPath("$.data[0].type").value("AUCTION_STARTED"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andDo(document("notification/get-notifications",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 알림 없음")
    void getNotifications_success_empty() throws Exception {
        // given
        given(notificationService.getNotifications(1L)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록을 조회했습니다"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }


    // ========================
    // 알림 읽음 처리
    // ========================

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_success() throws Exception {
        // given
        doNothing().when(notificationService).markAsRead(1L, 1L);

        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림을 읽음 처리했습니다"))
                .andDo(document("notification/mark-as-read",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("notificationId").description("알림 식별자"))
                ));

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 잘못된 notificationId 타입")
    void markAsRead_fail_invalidPathVariable() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", "invalid")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }


    // ========================
    // 전체 읽음 처리
    // ========================

    @Test
    @DisplayName("전체 읽음 처리 성공")
    void markAsReadAll_success() throws Exception {
        // given
        doNothing().when(notificationService).markAsReadAll(1L);

        // when & then
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("모든 알림을 읽음 처리했습니다"))
                .andDo(document("notification/mark-as-read-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));

        verify(notificationService).markAsReadAll(1L);
    }


    // ========================
    // 알림 삭제
    // ========================

    @Test
    @DisplayName("알림 삭제 성공")
    void delete_success() throws Exception {
        // given
        doNothing().when(notificationService).delete(1L, 1L);

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", 1L)
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림을 삭제했습니다"))
                .andDo(document("notification/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰")),
                        pathParameters(parameterWithName("notificationId").description("알림 식별자"))
                ));

        verify(notificationService).delete(1L, 1L);
    }

    @Test
    @DisplayName("알림 삭제 실패 - 잘못된 notificationId 타입")
    void delete_fail_invalidPathVariable() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", "invalid")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }


    // ========================
    // 알림 전체 삭제
    // ========================

    @Test
    @DisplayName("알림 전체 삭제 성공")
    void deleteAll_success() throws Exception {
        // given
        doNothing().when(notificationService).deleteAll(1L);

        // when & then
        mockMvc.perform(delete("/api/notifications")
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("모든 알림을 삭제했습니다"))
                .andDo(document("notification/delete-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName("Authorization").description("Bearer 액세스 토큰"))
                ));

        verify(notificationService).deleteAll(1L);
    }
}