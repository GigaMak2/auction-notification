package com.example.auctionnotification.notification.controller;

import com.example.auctionnotification.common.config.security.CustomUserDetails;
import com.example.auctionnotification.common.dto.BaseResponse;
import com.example.auctionnotification.notification.dto.NotificationResponse;
import com.example.auctionnotification.notification.service.NotificationService;
import com.example.auctionnotification.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return sseEmitterService.subscribe(userId);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "알림 목록 조회 요청 성공", notificationService.getNotifications(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "읽음 처리 요청 성공", null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        notificationService.delete(notificationId, userId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "알림 삭제 요청 성공", null));
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> deleteAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        notificationService.deleteAll(userId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "알림 전체 삭제 요청 성공", null));
    }
}
