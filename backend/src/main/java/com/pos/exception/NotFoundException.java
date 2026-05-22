package com.pos.exception;

/** Không tìm thấy tài nguyên → HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException("Không tìm thấy " + resource + " với id = " + id);
    }
}
