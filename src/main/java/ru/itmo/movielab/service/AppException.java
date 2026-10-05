package ru.itmo.movielab.service;

public class AppException extends RuntimeException {

    public final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }
}
