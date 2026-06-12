package com.equipe3.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /profiles/{name}/company. The company {@code type}, the player's
 * avatar name ({@code character}) and avatar picture key ({@code avatar}) are
 * optional choices made during initialization.
 */
public record CreateCompanyRequest(
        @NotBlank String name,
        String type,
        String character,
        String avatar) {
}
