package com.example.sports.provider;

public class CricApiException extends RuntimeException {

    public CricApiException(String message) {
        super(message);
    }

    public CricApiException(String message, Throwable cause) {
        super(message, cause);
    }
}