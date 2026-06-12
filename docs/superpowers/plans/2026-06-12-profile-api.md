# Profile API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a RESTful Profile API to the Spring Boot backend that persists profiles (name + base64 image) as JSON files on a configurable filesystem path.

**Architecture:** 3-layer (Controller → Service → Repository). `ProfileController` handles HTTP, `ProfileService` owns business logic, `ProfileRepository` handles all file I/O. Each layer is independently testable with mocked dependencies.

**Tech Stack:** Spring Boot 4.1.0, Java 21, Jackson (via spring-boot-starter-webmvc), JUnit 5 + Mockito + MockMvc (via spring-boot-starter-webmvc-test), AssertJ.

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `backend/src/main/java/com/equipe3/backend/profile/Profile.java` | Data record |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileNotFoundException.java` | 404 exception |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileAlreadyExistsException.java` | 409 exception |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileProperties.java` | Config binding |
| Modify | `backend/src/main/resources/application.yaml` | Add storage-dir config |
| Modify | `backend/src/main/java/com/equipe3/backend/BackendApplication.java` | Add @ConfigurationPropertiesScan |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileRepository.java` | File I/O |
| Create | `backend/src/test/java/com/equipe3/backend/profile/ProfileRepositoryTest.java` | Repository unit tests |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileService.java` | Business logic |
| Create | `backend/src/test/java/com/equipe3/backend/profile/ProfileServiceTest.java` | Service unit tests |
| Create | `backend/src/main/java/com/equipe3/backend/profile/ProfileController.java` | REST endpoints |
| Create | `backend/src/test/java/com/equipe3/backend/profile/ProfileControllerTest.java` | Controller unit tests |
| Modify | `backend/.gitignore` (or root `.gitignore`) | Ignore profiles-data/ |

---

### Task 1: Data model — Profile record and exception classes

These are pure data types with no logic. Create them first because every other task depends on them.

**Files:**
- Create: `backend/src/main/java/com/equipe3/backend/profile/Profile.java`
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileNotFoundException.java`
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileAlreadyExistsException.java`

- [ ] **Step 1: Create Profile.java**

```java
package com.equipe3.backend.profile;

public record Profile(String name, String image) {}
```

- [ ] **Step 2: Create ProfileNotFoundException.java**

```java
package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String name) {
        super("Profile not found: " + name);
    }
}
```

- [ ] **Step 3: Create ProfileAlreadyExistsException.java**

```java
package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(String name) {
        super("Profile already exists: " + name);
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd backend && ./mvnw compile -q
```
Expected: BUILD SUCCESS, no errors.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/equipe3/backend/profile/Profile.java \
        backend/src/main/java/com/equipe3/backend/profile/ProfileNotFoundException.java \
        backend/src/main/java/com/equipe3/backend/profile/ProfileAlreadyExistsException.java
git commit -m "feat: add Profile record and exception classes"
```

---

### Task 2: Configuration — ProfileProperties and application.yaml

**Files:**
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileProperties.java`
- Modify: `backend/src/main/resources/application.yaml`
- Modify: `backend/src/main/java/com/equipe3/backend/BackendApplication.java`

- [ ] **Step 1: Create ProfileProperties.java**

```java
package com.equipe3.backend.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profiles")
public record ProfileProperties(String storageDir) {}
```

- [ ] **Step 2: Update application.yaml**

Replace the full file content:
```yaml
spring:
  application:
    name: backend

profiles:
  storage-dir: ./profiles-data
```

- [ ] **Step 3: Update BackendApplication.java to enable config properties scanning**

```java
package com.equipe3.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
```

- [ ] **Step 4: Verify compilation**

```bash
cd backend && ./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/equipe3/backend/profile/ProfileProperties.java \
        backend/src/main/resources/application.yaml \
        backend/src/main/java/com/equipe3/backend/BackendApplication.java
git commit -m "feat: add ProfileProperties and storage-dir configuration"
```

---

### Task 3: ProfileRepository with TDD

**Files:**
- Create: `backend/src/test/java/com/equipe3/backend/profile/ProfileRepositoryTest.java`
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileRepository.java`

- [ ] **Step 1: Create the test file**

```java
package com.equipe3.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ProfileRepositoryTest {

    @TempDir
    Path tempDir;

    ProfileRepository repository;

    @BeforeEach
    void setUp() {
        ProfileProperties properties = new ProfileProperties(tempDir.toString());
        repository = new ProfileRepository(properties, new ObjectMapper());
    }

    @Test
    void saveAndFindByName_roundTrip() throws IOException {
        Profile profile = new Profile("acme", "base64image");
        repository.save(profile);
        Optional<Profile> found = repository.findByName("acme");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("acme");
        assertThat(found.get().image()).isEqualTo("base64image");
    }

    @Test
    void findByName_notFound_returnsEmpty() throws IOException {
        assertThat(repository.findByName("unknown")).isEmpty();
    }

    @Test
    void deleteByName_existing_returnsTrue() throws IOException {
        repository.save(new Profile("acme", "img"));
        assertThat(repository.deleteByName("acme")).isTrue();
        assertThat(repository.findByName("acme")).isEmpty();
    }

    @Test
    void deleteByName_notFound_returnsFalse() throws IOException {
        assertThat(repository.deleteByName("unknown")).isFalse();
    }

    @Test
    void save_duplicate_throwsProfileAlreadyExistsException() throws IOException {
        repository.save(new Profile("acme", "img"));
        assertThatThrownBy(() -> repository.save(new Profile("acme", "img2")))
                .isInstanceOf(ProfileAlreadyExistsException.class);
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (class does not exist yet)**

```bash
cd backend && ./mvnw test -pl . -Dtest=ProfileRepositoryTest -q 2>&1 | tail -5
```
Expected: COMPILATION FAILURE — `ProfileRepository` cannot be resolved.

- [ ] **Step 3: Create ProfileRepository.java**

```java
package com.equipe3.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Repository
public class ProfileRepository {

    private final Path storageDir;
    private final ObjectMapper objectMapper;

    public ProfileRepository(ProfileProperties properties, ObjectMapper objectMapper) {
        this.storageDir = Path.of(properties.storageDir());
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create profile storage directory: " + storageDir, e);
        }
    }

    public Profile save(Profile profile) throws IOException {
        Path file = filePath(profile.name());
        if (Files.exists(file)) {
            throw new ProfileAlreadyExistsException(profile.name());
        }
        objectMapper.writeValue(file.toFile(), profile);
        return profile;
    }

    public Optional<Profile> findByName(String name) throws IOException {
        Path file = filePath(name);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(file.toFile(), Profile.class));
    }

    public boolean deleteByName(String name) throws IOException {
        return Files.deleteIfExists(filePath(name));
    }

    private Path filePath(String name) {
        return storageDir.resolve(name + ".json");
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -Dtest=ProfileRepositoryTest -q
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/equipe3/backend/profile/ProfileRepository.java \
        backend/src/test/java/com/equipe3/backend/profile/ProfileRepositoryTest.java
git commit -m "feat: add ProfileRepository with file-system persistence"
```

---

### Task 4: ProfileService with TDD

**Files:**
- Create: `backend/src/test/java/com/equipe3/backend/profile/ProfileServiceTest.java`
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileService.java`

- [ ] **Step 1: Create the test file**

```java
package com.equipe3.backend.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    ProfileRepository repository;

    @InjectMocks
    ProfileService service;

    @Test
    void getByName_found_returnsProfile() throws IOException {
        Profile profile = new Profile("acme", "img");
        when(repository.findByName("acme")).thenReturn(Optional.of(profile));
        assertThat(service.getByName("acme")).contains(profile);
    }

    @Test
    void getByName_notFound_returnsEmpty() throws IOException {
        when(repository.findByName("unknown")).thenReturn(Optional.empty());
        assertThat(service.getByName("unknown")).isEmpty();
    }

    @Test
    void create_delegatesToRepository() throws IOException {
        Profile profile = new Profile("acme", "img");
        when(repository.save(profile)).thenReturn(profile);
        assertThat(service.create(profile)).isEqualTo(profile);
        verify(repository).save(profile);
    }

    @Test
    void deleteByName_found_doesNotThrow() throws IOException {
        when(repository.deleteByName("acme")).thenReturn(true);
        assertThatCode(() -> service.deleteByName("acme")).doesNotThrowAnyException();
    }

    @Test
    void deleteByName_notFound_throwsProfileNotFoundException() throws IOException {
        when(repository.deleteByName("unknown")).thenReturn(false);
        assertThatThrownBy(() -> service.deleteByName("unknown"))
                .isInstanceOf(ProfileNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -Dtest=ProfileServiceTest -q 2>&1 | tail -5
```
Expected: COMPILATION FAILURE — `ProfileService` cannot be resolved.

- [ ] **Step 3: Create ProfileService.java**

```java
package com.equipe3.backend.profile;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    public Profile create(Profile profile) throws IOException {
        return repository.save(profile);
    }

    public Optional<Profile> getByName(String name) throws IOException {
        return repository.findByName(name);
    }

    public void deleteByName(String name) throws IOException {
        boolean deleted = repository.deleteByName(name);
        if (!deleted) {
            throw new ProfileNotFoundException(name);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -Dtest=ProfileServiceTest -q
```
Expected: BUILD SUCCESS, 5 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/equipe3/backend/profile/ProfileService.java \
        backend/src/test/java/com/equipe3/backend/profile/ProfileServiceTest.java
git commit -m "feat: add ProfileService with business logic"
```

---

### Task 5: ProfileController with TDD

**Files:**
- Create: `backend/src/test/java/com/equipe3/backend/profile/ProfileControllerTest.java`
- Create: `backend/src/main/java/com/equipe3/backend/profile/ProfileController.java`

- [ ] **Step 1: Create the test file**

```java
package com.equipe3.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProfileService service;

    @Test
    void getProfile_found_returns200WithBody() throws Exception {
        when(service.getByName("acme")).thenReturn(Optional.of(new Profile("acme", "img")));
        mockMvc.perform(get("/profiles/acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.image").value("img"));
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        when(service.getByName("unknown")).thenReturn(Optional.empty());
        mockMvc.perform(get("/profiles/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProfile_returns201WithBody() throws Exception {
        Profile profile = new Profile("acme", "img");
        when(service.create(profile)).thenReturn(profile);
        mockMvc.perform(post("/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.image").value("img"));
    }

    @Test
    void createProfile_duplicate_returns409() throws Exception {
        Profile profile = new Profile("acme", "img");
        when(service.create(profile)).thenThrow(new ProfileAlreadyExistsException("acme"));
        mockMvc.perform(post("/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteProfile_found_returns204() throws Exception {
        doNothing().when(service).deleteByName("acme");
        mockMvc.perform(delete("/profiles/acme"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProfile_notFound_returns404() throws Exception {
        doThrow(new ProfileNotFoundException("unknown")).when(service).deleteByName("unknown");
        mockMvc.perform(delete("/profiles/unknown"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -Dtest=ProfileControllerTest -q 2>&1 | tail -5
```
Expected: COMPILATION FAILURE — `ProfileController` cannot be resolved.

- [ ] **Step 3: Create ProfileController.java**

```java
package com.equipe3.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/{name}")
    public ResponseEntity<Profile> getProfile(@PathVariable String name) throws IOException {
        return service.getByName(name)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProfileNotFoundException(name));
    }

    @PostMapping
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile) throws IOException {
        Profile created = service.create(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String name) throws IOException {
        service.deleteByName(name);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -Dtest=ProfileControllerTest -q
```
Expected: BUILD SUCCESS, 6 tests run, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/equipe3/backend/profile/ProfileController.java \
        backend/src/test/java/com/equipe3/backend/profile/ProfileControllerTest.java
git commit -m "feat: add ProfileController REST endpoints"
```

---

### Task 6: Final cleanup — gitignore and full test run

- [ ] **Step 1: Add profiles-data to .gitignore**

Open the root `.gitignore` (or `backend/.gitignore` if it exists) and add:
```
profiles-data/
```

- [ ] **Step 2: Run the full test suite**

```bash
cd backend && ./mvnw test -q
```
Expected: BUILD SUCCESS. All tests from `ProfileRepositoryTest`, `ProfileServiceTest`, `ProfileControllerTest`, and `BackendApplicationTests` pass (16 tests total, 0 failures).

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore: ignore profiles-data directory"
```
