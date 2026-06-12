package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(String name) {
        super("Profile already exists: " + name);
    }
}
