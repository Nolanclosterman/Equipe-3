package com.equipe3.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * A generated game event presented to the player. The textual part matches the
 * payload produced by {@code prompts/event_generation.md}:
 *
 * <pre>{ "problem": "", "solutions": [], "character": "", "lexicon": [] }</pre>
 *
 * The image fields ({@code illustration}, {@code characterImage},
 * {@code solutionIllustrations}) are filled in asynchronously by the
 * {@code IllustrationService} once the OpenAI image API returns; they stay
 * empty until then (or forever when image generation is disabled).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameEvent(
        String problem,
        List<String> solutions,
        String character,
        String characterImage,
        String illustration,
        List<String> solutionIllustrations,
        List<LexiconEntry> lexicon) {

    public GameEvent {
        solutions = solutions == null ? List.of() : List.copyOf(solutions);
        lexicon = lexicon == null ? List.of() : List.copyOf(lexicon);
        illustration = illustration == null ? "" : illustration;
        characterImage = characterImage == null ? "" : characterImage;
        if (solutionIllustrations == null || solutionIllustrations.size() != solutions.size()) {
            solutionIllustrations = solutions.stream().map(s -> "").toList();
        } else {
            solutionIllustrations = List.copyOf(solutionIllustrations);
        }
    }

    /** Convenience constructor for events without images yet. */
    public GameEvent(String problem, List<String> solutions, String character,
                     String illustration, List<LexiconEntry> lexicon) {
        this(problem, solutions, character, "", illustration, null, lexicon);
    }

    public GameEvent withIllustration(String url) {
        return new GameEvent(problem, solutions, character, characterImage, url,
                solutionIllustrations, lexicon);
    }

    public GameEvent withCharacterImage(String url) {
        return new GameEvent(problem, solutions, character, url, illustration,
                solutionIllustrations, lexicon);
    }

    public GameEvent withSolutionIllustration(int index, String url) {
        List<String> updated = new ArrayList<>(solutionIllustrations);
        if (index >= 0 && index < updated.size()) {
            updated.set(index, url);
        }
        return new GameEvent(problem, solutions, character, characterImage, illustration,
                updated, lexicon);
    }

    /** True when every expected image has been generated. */
    public boolean imagesComplete() {
        return !illustration.isEmpty()
                && !characterImage.isEmpty()
                && solutionIllustrations.stream().noneMatch(String::isEmpty);
    }
}
