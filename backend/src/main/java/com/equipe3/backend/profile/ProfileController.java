package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/{name}")
    public ResponseEntity<Profile> getProfile(@PathVariable String name) throws IOException {
        return service.getByName(name)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProfileNotFoundException(name));
    }

    @PostMapping
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile) throws IOException {
        Profile created = service.create(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String name) throws IOException {
        service.deleteByName(name);
        return ResponseEntity.noContent().build();
    }
}
