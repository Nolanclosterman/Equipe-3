package com.equipe3.backend.web;

import com.equipe3.backend.dto.ChatRequest;
import com.equipe3.backend.dto.CompanyDto;
import com.equipe3.backend.dto.CreateCompanyRequest;
import com.equipe3.backend.dto.DecisionRequest;
import com.equipe3.backend.dto.ProfileResponse;
import com.equipe3.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints backing the mockup:
 *   GET    /profiles/{name}                  -> connected profile + active company
 *   POST   /profiles/{name}/company          -> create a company (if none exists)
 *   POST   /profiles/{name}/company/start     -> start the existing company
 *   DELETE /profiles/{name}/company          -> delete the existing company
 * plus the dashboard actions (chat, icon generation).
 */
@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/{name}")
    public ProfileResponse getProfile(@PathVariable String name) {
        return ProfileResponse.from(service.getProfile(name));
    }

    @PostMapping("/{name}/company")
    public ResponseEntity<CompanyDto> createCompany(
            @PathVariable String name,
            @Valid @RequestBody CreateCompanyRequest request) {
        CompanyDto company = CompanyDto.from(
                service.createCompany(name, request.name(), request.type(),
                        request.character(), request.avatar()));
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @PostMapping("/{name}/company/start")
    public CompanyDto startCompany(@PathVariable String name) {
        return CompanyDto.from(service.startCompany(name));
    }

    @DeleteMapping("/{name}/company")
    public ResponseEntity<Void> deleteCompany(@PathVariable String name) {
        service.deleteCompany(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{name}/company/chat")
    public CompanyDto chat(
            @PathVariable String name,
            @Valid @RequestBody ChatRequest request) {
        return CompanyDto.from(service.addChatMessage(name, request.message()));
    }

    @PostMapping("/{name}/company/icon")
    public CompanyDto generateIcon(@PathVariable String name) {
        return CompanyDto.from(service.generateIcon(name));
    }

    @PostMapping("/{name}/company/event")
    public CompanyDto generateEvent(@PathVariable String name) {
        return CompanyDto.from(service.generateEvent(name));
    }

    @PostMapping("/{name}/company/decision")
    public CompanyDto decide(
            @PathVariable String name,
            @Valid @RequestBody DecisionRequest request) {
        return CompanyDto.from(service.decide(name, request.solution()));
    }
}
