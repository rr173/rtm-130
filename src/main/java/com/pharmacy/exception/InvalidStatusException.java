package com.pharmacy.exception;

public class InvalidStatusException extends BusinessException {

    public InvalidStatusException(String message) {
        super(409, message);
    }
}
