package com.equipe3.backend.model;

/**
 * A player profile, identified by name. A profile owns at most one company
 * ("1 seule entreprise par joueur").
 */
public class Profile {

    private final String name;
    private Company company;

    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public boolean hasCompany() {
        return company != null;
    }
}
