package com.equipe3.backend.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProfileService service;

    @Test
    void getProfile_found_returns200WithBody() throws Exception {
        when(service.getByName("acme")).thenReturn(Optional.of(new Profile("acme", "img")));
        mockMvc.perform(get("/profiles/acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.image").value("img"));
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        when(service.getByName("unknown")).thenReturn(Optional.empty());
        mockMvc.perform(get("/profiles/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProfile_returns201WithBody() throws Exception {
        Profile profile = new Profile("acme", "img");
        when(service.create(profile)).thenReturn(profile);
        mockMvc.perform(post("/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.image").value("img"));
    }

    @Test
    void createProfile_duplicate_returns409() throws Exception {
        Profile profile = new Profile("acme", "img");
        when(service.create(profile)).thenThrow(new ProfileAlreadyExistsException("acme"));
        mockMvc.perform(post("/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteProfile_found_returns204() throws Exception {
        doNothing().when(service).deleteByName("acme");
        mockMvc.perform(delete("/profiles/acme"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProfile_notFound_returns404() throws Exception {
        doThrow(new ProfileNotFoundException("unknown")).when(service).deleteByName("unknown");
        mockMvc.perform(delete("/profiles/unknown"))
                .andExpect(status().isNotFound());
    }
}
