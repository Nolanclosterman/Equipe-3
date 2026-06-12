package com.equipe3.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Stores the generated PNG assets on disk and resolves them back for the
 * {@code GET /assets/{file}} endpoint. URLs handed to the frontend are
 * root-relative ({@code /assets/...}) so they work both behind the Angular dev
 * proxy and behind Nginx in Docker.
 */
@Service
public class AssetStorageService {

    private static final Logger log = LoggerFactory.getLogger(AssetStorageService.class);

    private final Path directory;

    public AssetStorageService(@Value("${assets.dir:./generated-assets}") String dir) {
        this.directory = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create assets directory " + directory, e);
        }
        log.info("Generated assets stored in {}", directory);
    }

    /** Saves a PNG under the given file name and returns its public URL path. */
    public String save(String fileName, byte[] png) {
        try {
            Files.write(directory.resolve(safe(fileName)), png);
            return url(fileName);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write asset " + fileName, e);
        }
    }

    public boolean exists(String fileName) {
        return Files.exists(directory.resolve(safe(fileName)));
    }

    public String url(String fileName) {
        // The frontend reaches the backend through the /api proxy (Angular dev
        // server and Nginx both strip the prefix), so image URLs carry it too.
        return "/api/assets/" + safe(fileName);
    }

    /** Resolves an asset for serving; empty when missing or path is invalid. */
    public Optional<Path> resolve(String fileName) {
        try {
            Path path = directory.resolve(safe(fileName)).normalize();
            if (path.startsWith(directory) && Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        } catch (IllegalArgumentException ignored) {
            // invalid name — treated as not found
        }
        return Optional.empty();
    }

    /** Allows only flat, simple file names (defense against path traversal). */
    private static String safe(String fileName) {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9._-]+") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid asset name: " + fileName);
        }
        return fileName;
    }
}
