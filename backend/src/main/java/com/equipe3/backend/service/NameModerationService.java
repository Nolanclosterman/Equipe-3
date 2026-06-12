package com.equipe3.backend.service;

import com.equipe3.backend.web.ApiExceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Set;

/**
 * Guardrail for the free-text the player can type (company name, custom
 * activity, avatar name): rejects inappropriate words before they reach the
 * game and the AI prompts. The check normalizes accents and simple leetspeak
 * (P0rn0 -> porno) and matches a blocklist of French/English profanity —
 * exact word matches for short terms (so "calcul" or "réputation" stay valid)
 * and substring matches for longer, unambiguous ones.
 *
 * This is a fast first barrier for a kids' game, not a perfect filter; the AI
 * prompts keep their own guardrails for everything generated during play.
 */
@Service
public class NameModerationService {

    /** Matched as whole words only (short terms that appear inside real words). */
    private static final Set<String> BLOCKED_WORDS = Set.of(
            // French
            "merde", "pute", "putes", "salope", "connard", "connards", "connasse",
            "batard", "batards", "bite", "bites", "couille", "couilles", "cul",
            "chier", "chiotte", "pd", "fdp", "ntm", "tg", "nique", "niquer",
            "niquee", "enfoire", "bordel", "penis", "vagin", "sexe", "porno",
            "negre", "negro", "bougnoule", "youpin", "pedo", "pedophile",
            // English
            "fuck", "fucking", "shit", "bitch", "asshole", "dick", "cock",
            "pussy", "whore", "slut", "sex", "porn", "nude", "boobs",
            "nazi", "hitler");

    /** Matched anywhere in the text (long, unambiguous fragments). */
    private static final Set<String> BLOCKED_FRAGMENTS = Set.of(
            "encul", "putain", "salop", "fuck", "shit", "bitch",
            "porno", "nazi", "hitler", "pedophil");

    /**
     * Validates a player-typed text; throws a kid-friendly 400 when it
     * contains inappropriate words. Null/blank values are accepted (presence
     * is validated elsewhere).
     */
    public void validate(String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = normalize(value);
        String joined = normalized.replaceAll("[^a-z]", "");
        for (String fragment : BLOCKED_FRAGMENTS) {
            if (joined.contains(fragment)) {
                throw reject(label);
            }
        }
        for (String word : normalized.split("[^a-z]+")) {
            if (BLOCKED_WORDS.contains(word)) {
                throw reject(label);
            }
        }
    }

    private static BadRequestException reject(String label) {
        return new BadRequestException(
                "Oups, " + label + " contient un mot qui n'a pas sa place dans le jeu. "
                        + "Choisis-en un autre ! 😊");
    }

    /** Lowercase, strip accents and undo simple leetspeak digit substitutions. */
    private static String normalize(String value) {
        String text = Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return text
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't')
                .replace('@', 'a')
                .replace('$', 's');
    }
}
