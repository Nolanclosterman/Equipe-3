package com.equipe3.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The narrative consequence of a player's decision and the resulting changes to
 * the three indicators, produced by {@code prompts/event_scoring.md}. Deltas are
 * applied to the company scores (which are clamped to 0..100).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventOutcome(
        String narrative,
        int moneyDelta,
        int ecologyDelta,
        int ethicsDelta) {
}
