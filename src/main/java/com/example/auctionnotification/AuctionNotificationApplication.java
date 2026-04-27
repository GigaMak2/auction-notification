package com.example.auctionnotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AuctionNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionNotificationApplication.class, args);
    }

}
