package com.tanigawa.rewardplatform.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "User not found."
    ),

    WALLET_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "Wallet not found."
    ),

    REWARD_EVENT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "Reward event not found."
    ),

    REWARD_HISTORY_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "Reward history not found"
    ),

    REWARD_DISABLED(
        HttpStatus.BAD_REQUEST,
        "Reward event is disabled."
    ),

    CONCURRENT_WORK(
        HttpStatus.BAD_REQUEST,
        "transaction is asked concurrently."
    ),

    INVALID_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "Invalid JWT token."
    ),

    UNAUTHORIZED(
        HttpStatus.UNAUTHORIZED,
        "Authentication is required."
    ),

    INVALID_CREDENTIALS(
        HttpStatus.UNAUTHORIZED,
        "Email or Password is wrong."
    ),

    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "Access denied."
    ),

    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected server error."
    );

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}