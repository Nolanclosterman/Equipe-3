package com.equipe3.backend.profile;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Repository
public class ProfileRepository {

    private final Path storageDir;
    private final ObjectMapper objectMapper;

    public ProfileRepository(ProfileProperties properties, ObjectMapper objectMapper) {
        this.storageDir = Path.of(properties.storageDir()).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create profile storage directory: " + storageDir, e);
        }
    }

    public Profile save(Profile profile) throws IOException {
        Path file = filePath(profile.name());
        try (var out = Files.newOutputStream(file, java.nio.file.StandardOpenOption.CREATE_NEW)) {
            objectMapper.writeValue(out, profile);
        } catch (java.nio.file.FileAlreadyExistsException e) {
            throw new ProfileAlreadyExistsException(profile.name());
        }
        return profile;
    }

    public Optional<Profile> findByName(String name) throws IOException {
        Path file = filePath(name);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(file.toFile(), Profile.class));
    }

    public boolean deleteByName(String name) throws IOException {
        return Files.deleteIfExists(filePath(name));
    }

    private Path filePath(String name) {
        Path resolved = storageDir.resolve(name + ".json").normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid profile name: " + name);
        }
        return resolved;
    }
}
