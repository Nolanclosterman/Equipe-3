package com.equipe3.backend.profile;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    public Profile create(Profile profile) throws IOException {
        return repository.save(profile);
    }

    public Optional<Profile> getByName(String name) throws IOException {
        return repository.findByName(name);
    }

    public void deleteByName(String name) throws IOException {
        boolean deleted = repository.deleteByName(name);
        if (!deleted) {
            throw new ProfileNotFoundException(name);
        }
    }
}
