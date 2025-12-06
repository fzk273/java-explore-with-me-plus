package ru.practicum.exception;

public class RepeatableUserRequestException extends RuntimeException {
    public RepeatableUserRequestException(String message) {
        super(message);
    }
}
