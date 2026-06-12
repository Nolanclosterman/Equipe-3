package com.equipe3.backend.repository;

import com.equipe3.backend.model.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of profiles keyed by name. A real persistence layer can be
 * swapped in later without touching the service or controller.
 */
@Repository
public class ProfileRepository {

    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

    public Optional<Profile> findByName(String name) {
        return Optional.ofNullable(profiles.get(key(name)));
    }

    /** Returns the existing profile or creates an empty one on first access. */
    public Profile findOrCreate(String name) {
        return profiles.computeIfAbsent(key(name), k -> new Profile(name));
    }

    public Profile save(Profile profile) {
        profiles.put(key(profile.getName()), profile);
        return profile;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
