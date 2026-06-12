package com.equipe3.backend.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    ProfileRepository repository;

    @InjectMocks
    ProfileService service;

    @Test
    void getByName_found_returnsProfile() throws IOException {
        Profile profile = new Profile("acme", "img");
        when(repository.findByName("acme")).thenReturn(Optional.of(profile));
        assertThat(service.getByName("acme")).contains(profile);
    }

    @Test
    void getByName_notFound_returnsEmpty() throws IOException {
        when(repository.findByName("unknown")).thenReturn(Optional.empty());
        assertThat(service.getByName("unknown")).isEmpty();
    }

    @Test
    void create_delegatesToRepository() throws IOException {
        Profile profile = new Profile("acme", "img");
        when(repository.save(profile)).thenReturn(profile);
        assertThat(service.create(profile)).isEqualTo(profile);
        verify(repository).save(profile);
    }

    @Test
    void deleteByName_found_doesNotThrow() throws IOException {
        when(repository.deleteByName("acme")).thenReturn(true);
        assertThatCode(() -> service.deleteByName("acme")).doesNotThrowAnyException();
        verify(repository).deleteByName("acme");
    }

    @Test
    void deleteByName_notFound_throwsProfileNotFoundException() throws IOException {
        when(repository.deleteByName("unknown")).thenReturn(false);
        assertThatThrownBy(() -> service.deleteByName("unknown"))
                .isInstanceOf(ProfileNotFoundException.class);
    }
}
