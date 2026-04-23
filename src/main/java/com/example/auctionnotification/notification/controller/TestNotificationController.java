package com.example.auctionnotification.notification.controller;

import com.example.auctionnotification.notification.service.TestNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestNotificationController {

    private final TestNotificationService testNotificationService;

    @PostMapping("/platform-thread")
    public ResponseEntity<Void> testPlatformThread() {
        testNotificationService.simulatePlatformThread();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/virtual-thread")
    public ResponseEntity<Void> testVirtualThread() {
        testNotificationService.simulateVirtualThread();
        return ResponseEntity.ok().build();
    }
}
