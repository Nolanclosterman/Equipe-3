package com.equipe3.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /profiles/{name}/company. */
public record CreateCompanyRequest(@NotBlank String name) {
}
