package com.seii.legal.data.repository;

/** The write conflicts with persisted data. The surrounding Spring transaction should roll back. */
public class DataConflictException extends RuntimeException {
    public DataConflictException(String message) {
        super(message);
    }
}
