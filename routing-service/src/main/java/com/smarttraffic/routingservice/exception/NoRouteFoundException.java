package com.smarttraffic.routingservice.exception;

public class NoRouteFoundException extends RuntimeException {

    public NoRouteFoundException(String message) {
        super(message);
    }

}