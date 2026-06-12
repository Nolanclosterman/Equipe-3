package com.equipe3.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A generated game event presented to the player. Matches the payload produced
 * by {@code prompts/event_generation.md}:
 *
 * <pre>{ "problem": "", "solutions": [], "character": "", "illustration": "" }</pre>
 *
 * plus an educational {@code lexicon}. The illustration is skipped for now and
 * stays empty.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameEvent(
        String problem,
        List<String> solutions,
        String character,
        String illustration,
        List<LexiconEntry> lexicon) {

    public GameEvent {
        solutions = solutions == null ? List.of() : List.copyOf(solutions);
        lexicon = lexicon == null ? List.of() : List.copyOf(lexicon);
        illustration = illustration == null ? "" : illustration;
    }
}
