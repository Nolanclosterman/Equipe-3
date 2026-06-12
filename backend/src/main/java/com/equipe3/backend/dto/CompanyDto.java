package com.equipe3.backend.dto;

import com.equipe3.backend.model.ChatMessage;
import com.equipe3.backend.model.Company;
import com.equipe3.backend.model.EventOutcome;
import com.equipe3.backend.model.GameEvent;

import java.util.List;

/** The active company and its current state, as shown on the dashboard / game screen. */
public record CompanyDto(
        String name,
        String type,
        String character,
        boolean active,
        String iconUrl,
        ScoresDto scores,
        List<ChatMessage> chat,
        GameEvent currentEvent,
        EventOutcome lastOutcome,
        int eventNumber,
        int maxEvents,
        boolean gameOver) {

    public static CompanyDto from(Company company) {
        return new CompanyDto(
                company.getName(),
                company.getType(),
                company.getCharacter(),
                company.isActive(),
                company.getIconUrl(),
                ScoresDto.from(company.getScores()),
                List.copyOf(company.getChat()),
                company.getCurrentEvent(),
                company.getLastOutcome(),
                company.getEventNumber(),
                Company.MAX_EVENTS,
                company.isGameOver());
    }
}
