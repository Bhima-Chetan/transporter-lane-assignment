package com.transporter.assignment.exception;

public class InputNotFoundException extends RuntimeException {

    public InputNotFoundException() {
        super("No input data found. Submit lanes and quotes to POST /api/v1/transporters/input first.");
    }
}
