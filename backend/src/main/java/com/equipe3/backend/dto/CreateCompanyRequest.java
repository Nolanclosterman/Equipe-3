package com.equipe3.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /profiles/{name}/company. The company {@code type} and player
 * {@code character} (avatar) are optional choices made during initialization.
 */
public record CreateCompanyRequest(
        @NotBlank String name,
        String type,
        String character) {
}
