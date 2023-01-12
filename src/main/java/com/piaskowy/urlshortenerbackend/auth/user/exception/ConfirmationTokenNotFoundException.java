package com.piaskowy.urlshortenerbackend.auth.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConfirmationTokenNotFoundException extends RuntimeException {
    public ConfirmationTokenNotFoundException(final String message) {
        super(message);
    }
}
