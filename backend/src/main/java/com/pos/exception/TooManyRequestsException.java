package com.pos.exception;

/** Vượt giới hạn thử (vd đăng nhập sai quá nhiều lần) → HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
