package com.equipe3.backend.dto;

import com.equipe3.backend.model.Profile;

/**
 * Response of GET /profiles/{name}: the connected profile and its active
 * company (null when the player has not created one yet -> "CAS 1").
 */
public record ProfileResponse(String name, CompanyDto company) {

    public static ProfileResponse from(Profile profile) {
        CompanyDto company = profile.hasCompany() ? CompanyDto.from(profile.getCompany()) : null;
        return new ProfileResponse(profile.getName(), company);
    }
}
