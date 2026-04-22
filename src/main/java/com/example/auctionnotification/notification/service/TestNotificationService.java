package com.example.auctionnotification.notification.service;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Profile("!prod")
@Service
public class TestNotificationService {

    @Async("notificationExecutor")
    public void simulatePlatformThread() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Async("notificationExecutorWithVT")
    public void simulateVirtualThread() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
