package com.example.auctionnotification.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorEnumInterface {
    HttpStatus getStatus();
    String getMessage();
}
