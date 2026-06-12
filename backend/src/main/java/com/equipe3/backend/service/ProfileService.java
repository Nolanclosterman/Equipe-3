package com.equipe3.backend.service;

import com.equipe3.backend.model.ChatMessage;
import com.equipe3.backend.model.Company;
import com.equipe3.backend.model.Profile;
import com.equipe3.backend.repository.ProfileRepository;
import com.equipe3.backend.web.ApiExceptions.ConflictException;
import com.equipe3.backend.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;

/**
 * Business rules around the single company a player can own, matching the two
 * cases of Frame 1 in the mockup (no company yet / existing company) and the
 * dashboard of Frame 2.
 */
@Service
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    /** GET /profiles/{name} — returns (and lazily creates) the connected profile. */
    public Profile getProfile(String name) {
        return repository.findOrCreate(name);
    }

    /** POST /profiles/{name}/company — create a company only if none exists. */
    public Company createCompany(String name, String companyName) {
        Profile profile = repository.findOrCreate(name);
        if (profile.hasCompany()) {
            throw new ConflictException("Profile '" + name + "' already owns a company.");
        }
        Company company = new Company(companyName);
        profile.setCompany(company);
        repository.save(profile);
        return company;
    }

    /** POST /profiles/{name}/company/start — activate the existing company. */
    public Company startCompany(String name) {
        Company company = requireCompany(name);
        company.setActive(true);
        if (company.getChat().isEmpty()) {
            company.getChat().add(ChatMessage.assistant(
                    "Salut ! Bienvenue à la tête de « " + company.getName()
                            + " ». Comment puis-je t'aider aujourd'hui ?"));
        }
        return company;
    }

    /** DELETE /profiles/{name}/company — remove the existing company. */
    public void deleteCompany(String name) {
        Profile profile = requireProfile(name);
        if (!profile.hasCompany()) {
            throw new NotFoundException("Profile '" + name + "' has no company to delete.");
        }
        profile.setCompany(null);
        repository.save(profile);
    }

    /** POST /profiles/{name}/company/chat — append a player message and a reply. */
    public Company addChatMessage(String name, String message) {
        Company company = requireCompany(name);
        company.getChat().add(ChatMessage.user(message));
        company.getChat().add(ChatMessage.assistant(
                "Bonne question ! (réponse de l'assistant à brancher sur l'IA)"));
        return company;
    }

    /** POST /profiles/{name}/company/icon — (re)generate the company icon. */
    public Company generateIcon(String name) {
        Company company = requireCompany(name);
        // Placeholder deterministic icon based on the company name; swap for the
        // real generative asset service later.
        String seed = company.getName() == null ? "company" : company.getName().trim();
        company.setIconUrl("https://api.dicebear.com/9.x/shapes/svg?seed="
                + java.net.URLEncoder.encode(seed, java.nio.charset.StandardCharsets.UTF_8));
        return company;
    }

    private Profile requireProfile(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Profile '" + name + "' not found."));
    }

    private Company requireCompany(String name) {
        Profile profile = requireProfile(name);
        if (!profile.hasCompany()) {
            throw new NotFoundException("Profile '" + name + "' has no company.");
        }
        return profile.getCompany();
    }
}
