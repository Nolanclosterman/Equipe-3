package com.equipe3.backend.model;

import java.time.Instant;

/**
 * A single message in the assistant chat. The author is either the player
 * ("user") or the AI game master ("assistant").
 */
public record ChatMessage(String author, String content, Instant timestamp) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, Instant.now());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, Instant.now());
    }
}
