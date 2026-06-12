package com.equipe3.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single company a player owns. Holds the indicators, the generated icon,
 * the assistant chat history and the game-loop state (current event, decision
 * history, outcome of the last decision). A company starts inactive until the
 * player presses "Démarrer".
 */
public class Company {

    /** A session lasts at most 15 events (Design §7.1). */
    public static final int MAX_EVENTS = 15;

    private String name;
    private String type;
    private String character;
    private boolean active;
    private String iconUrl;
    private final Scores scores = new Scores();
    private final List<ChatMessage> chat = new ArrayList<>();

    // --- game loop state ---
    private GameEvent currentEvent;
    private EventOutcome lastOutcome;
    private int eventNumber;
    private boolean gameOver;
    private final List<DecisionRecord> history = new ArrayList<>();

    /**
     * Portraits of the story characters, keyed by a slug of the character
     * description. Cached so a recurring character keeps the same face across
     * events. Concurrent because the IllustrationService fills it off-thread.
     */
    private final Map<String, String> characterImages = new ConcurrentHashMap<>();

    public Company() {
    }

    public Company(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Scores getScores() {
        return scores;
    }

    public List<ChatMessage> getChat() {
        return chat;
    }

    public GameEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(GameEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public EventOutcome getLastOutcome() {
        return lastOutcome;
    }

    public void setLastOutcome(EventOutcome lastOutcome) {
        this.lastOutcome = lastOutcome;
    }

    public int getEventNumber() {
        return eventNumber;
    }

    public void setEventNumber(int eventNumber) {
        this.eventNumber = eventNumber;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public List<DecisionRecord> getHistory() {
        return history;
    }

    public Map<String, String> getCharacterImages() {
        return characterImages;
    }
}
