package com.equipe3.backend.dto;

import com.equipe3.backend.model.Scores;

/** The three indicators exposed to the dashboard. */
public record ScoresDto(int money, int ecology, int ethics) {

    public static ScoresDto from(Scores scores) {
        return new ScoresDto(scores.getMoney(), scores.getEcology(), scores.getEthics());
    }
}
