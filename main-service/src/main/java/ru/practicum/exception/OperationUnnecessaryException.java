package ru.practicum.exception;

public class OperationUnnecessaryException extends RuntimeException {
    public OperationUnnecessaryException(String message) {
        super(message);
    }
}
