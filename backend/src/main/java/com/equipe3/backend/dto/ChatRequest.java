package com.equipe3.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /profiles/{name}/company/chat. */
public record ChatRequest(@NotBlank String message) {
}
