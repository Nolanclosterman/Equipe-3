package com.equipe3.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /profiles/{name}/company/decision: the solution the player chose. */
public record DecisionRequest(@NotBlank String solution) {
}
