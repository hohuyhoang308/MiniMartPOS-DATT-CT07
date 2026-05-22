package com.pos.exception;

/** Xung đột trạng thái (trùng dữ liệu, hết tồn khi bán...) → HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
