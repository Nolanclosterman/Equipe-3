package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String name) {
        super("Profile not found: " + name);
    }
}
