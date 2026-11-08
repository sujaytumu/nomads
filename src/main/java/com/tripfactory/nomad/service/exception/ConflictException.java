package com.tripfactory.nomad.service.exception;

/** The thing being changed was changed by someone else since it was loaded. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
