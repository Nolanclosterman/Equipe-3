package com.equipe3.backend.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The single company a player owns. Holds the indicators, the generated icon
 * and the assistant chat history. A company starts inactive until the player
 * presses "Démarrer".
 */
public class Company {

    private String name;
    private boolean active;
    private String iconUrl;
    private final Scores scores = new Scores();
    private final List<ChatMessage> chat = new ArrayList<>();

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
}
