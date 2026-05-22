package com.pos.exception;

/** Dữ liệu/nghiệp vụ không hợp lệ → HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
