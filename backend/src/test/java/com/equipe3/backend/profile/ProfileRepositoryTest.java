package com.equipe3.backend.profile;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ProfileRepositoryTest {

    @TempDir
    Path tempDir;

    ProfileRepository repository;

    @BeforeEach
    void setUp() {
        ProfileProperties properties = new ProfileProperties(tempDir.toString());
        repository = new ProfileRepository(properties, new ObjectMapper());
    }

    @Test
    void saveAndFindByName_roundTrip() throws IOException {
        Profile profile = new Profile("acme", "base64image");
        repository.save(profile);
        Optional<Profile> found = repository.findByName("acme");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("acme");
        assertThat(found.get().image()).isEqualTo("base64image");
    }

    @Test
    void findByName_notFound_returnsEmpty() throws IOException {
        assertThat(repository.findByName("unknown")).isEmpty();
    }

    @Test
    void deleteByName_existing_returnsTrue() throws IOException {
        repository.save(new Profile("acme", "img"));
        assertThat(repository.deleteByName("acme")).isTrue();
        assertThat(repository.findByName("acme")).isEmpty();
    }

    @Test
    void deleteByName_notFound_returnsFalse() throws IOException {
        assertThat(repository.deleteByName("unknown")).isFalse();
    }

    @Test
    void save_duplicate_throwsProfileAlreadyExistsException() throws IOException {
        repository.save(new Profile("acme", "img"));
        assertThatThrownBy(() -> repository.save(new Profile("acme", "img2")))
                .isInstanceOf(ProfileAlreadyExistsException.class);
    }

    @Test
    void save_pathTraversal_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> repository.save(new Profile("../../evil", "img")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
