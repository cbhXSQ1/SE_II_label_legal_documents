package com.seii.legal.data.repository;

public class DataConflictException extends RuntimeException {
    public DataConflictException(String message) {
        super(message);
    }
}
