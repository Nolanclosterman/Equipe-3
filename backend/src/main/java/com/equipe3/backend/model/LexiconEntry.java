package com.equipe3.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One technical term and its kid-friendly definition, added to events for
 * educational purposes (see Design §4.1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LexiconEntry(String term, String definition) {
}
