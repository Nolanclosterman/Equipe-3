# Profile API — Design Spec

**Date:** 2026-06-12  
**Status:** Approved

---

## Overview

Add a RESTful Profile API to the Spring Boot backend. Profiles have a name and an image (stored/transmitted as base64). Profiles are persisted as JSON files on the local filesystem at a configurable path.

---

## Data Model

A profile is a simple value object:

```
name  : String  — unique identifier and display name
image : String  — base64-encoded image data
```

Each profile is stored as a single JSON file: `{storage-dir}/{name}.json`

```json
{ "name": "acme", "image": "<base64-encoded-string>" }
```

The filename is the profile name (as provided). The `name` field inside the file is the canonical value returned in API responses.

---

## API Endpoints

| Method   | Path               | Request Body                          | Success Response                   | Error Responses         |
|----------|--------------------|---------------------------------------|------------------------------------|-------------------------|
| `POST`   | `/profiles`        | `{"name":"...","image":"<base64>"}`   | `201 Created` + profile JSON body  | `409 Conflict` if name already exists |
| `GET`    | `/profiles/{name}` | —                                     | `200 OK` + profile JSON body       | `404 Not Found`         |
| `DELETE` | `/profiles/{name}` | —                                     | `204 No Content`                   | `404 Not Found`         |

**Profile JSON shape** (used in both request and response):
```json
{
  "name": "acme",
  "image": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

---

## Architecture: 3-Layer (Controller → Service → Repository)

### `Profile`
Plain Java record:
```java
public record Profile(String name, String image) {}
```

### `ProfileProperties`
`@ConfigurationProperties(prefix = "profiles")` binding `storage-dir` from `application.yaml`. Validated with `@NotBlank`.

### `ProfileRepository`
Responsible for all file I/O under the configured directory. Operations:
- `save(Profile)` — write `{name}.json`; throw `ProfileAlreadyExistsException` if file exists
- `findByName(String)` — return `Optional<Profile>`; read and deserialize file if present
- `deleteByName(String)` — delete file; return `false` if not found

Uses Jackson `ObjectMapper` for JSON serialization. File names match the profile name exactly.

### `ProfileService`
Business logic layer; delegates to `ProfileRepository`:
- `create(Profile)` — delegates to `repository.save()`; propagates `ProfileAlreadyExistsException`
- `getByName(String)` — returns `Optional<Profile>` from `repository.findByName()`
- `deleteByName(String)` — calls `repository.deleteByName()`; throws `ProfileNotFoundException` if not found

### `ProfileController`
`@RestController` at base path `/profiles`. Delegates to `ProfileService`. Maps exceptions to HTTP status codes via `@ExceptionHandler` or `@ResponseStatus` on exception classes:
- `ProfileNotFoundException` → `404`
- `ProfileAlreadyExistsException` → `409`

### Exception Classes
- `ProfileNotFoundException` — thrown by service on missing profile
- `ProfileAlreadyExistsException` — thrown by repository on duplicate create

---

## Configuration

`application.yaml`:
```yaml
spring:
  application:
    name: backend

profiles:
  storage-dir: ./profiles-data
```

---

## File Structure

```
src/main/java/com/equipe3/backend/
  profile/
    Profile.java
    ProfileProperties.java
    ProfileRepository.java
    ProfileService.java
    ProfileController.java
    ProfileNotFoundException.java
    ProfileAlreadyExistsException.java

src/test/java/com/equipe3/backend/
  profile/
    ProfileRepositoryTest.java
    ProfileServiceTest.java
    ProfileControllerTest.java
```

---

## Testing Strategy

### `ProfileRepositoryTest`
- Uses JUnit 5 `@TempDir` to test against a real temporary directory
- Covers: save and read back, read non-existent profile returns empty, delete existing, delete non-existent returns false, save duplicate throws exception

### `ProfileServiceTest`
- Pure unit test; `ProfileRepository` is mocked with Mockito
- Covers: get found, get not found returns empty, create delegates correctly, delete found, delete not found throws `ProfileNotFoundException`

### `ProfileControllerTest`
- `@WebMvcTest(ProfileController.class)` with `@MockitoBean ProfileService`
- Covers: GET 200 with correct JSON, GET 404, POST 201 with location/body, POST 409 conflict, DELETE 204, DELETE 404
