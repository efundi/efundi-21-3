package edu.nwu.sakaistudentlink.services;

@SuppressWarnings("serial")
public class ConnectionNotEstablishedException extends Exception {

    public ConnectionNotEstablishedException(String message, Throwable cause) {
        super(message, cause);
    }
}