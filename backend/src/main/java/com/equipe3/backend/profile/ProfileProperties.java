package com.equipe3.backend.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profiles")
public record ProfileProperties(String storageDir) {}
