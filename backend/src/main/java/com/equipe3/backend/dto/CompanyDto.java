package com.equipe3.backend.dto;

import com.equipe3.backend.model.ChatMessage;
import com.equipe3.backend.model.Company;

import java.util.List;

/** The active company and its current state, as shown on the dashboard. */
public record CompanyDto(
        String name,
        boolean active,
        String iconUrl,
        ScoresDto scores,
        List<ChatMessage> chat) {

    public static CompanyDto from(Company company) {
        return new CompanyDto(
                company.getName(),
                company.isActive(),
                company.getIconUrl(),
                ScoresDto.from(company.getScores()),
                List.copyOf(company.getChat()));
    }
}
