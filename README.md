# Equipe-3

Application split into two parts:

- `frontend/`: Angular single-page application
- `backend/`: Spring Boot REST API

The frontend lets a player manage a company, start it, delete it, generate an icon, and chat with the assistant. The backend exposes the REST endpoints and keeps the profile/company state used by the UI.

## Project structure

```text
Equipe-3/
├── backend/          # Spring Boot API
├── frontend/         # Angular app
├── docker-compose.yml
├── Design.md
└── README.md
```

## Architecture overview

### Frontend (`frontend/`)

The frontend is an Angular 20 application.

Main responsibilities:
- load the connected player profile on startup
- create / start / delete a company
- display company scores (`money`, `ecology`, `ethics`)
- generate a company icon
- send and display chat messages

Important frontend files:
- `frontend/src/app/app.ts`: application entry component
- `frontend/src/app/services/profile.service.ts`: central API/state service
- `frontend/src/app/components/company/`: company creation/start/delete view
- `frontend/src/app/components/dashboard/`: dashboard with scores, icon, and chat

Runtime behavior:
- in local development (`ng serve` on port `4200`), the Angular dev server proxies `/api` to `http://localhost:8080`
- in Docker, the frontend is served by Nginx and `/api` is proxied to the backend container

### Backend (`backend/`)

The backend is a Spring Boot 4 application running on Java 21.

Main responsibilities:
- expose REST endpoints for the frontend
- return the connected profile
- create, start, and delete a company
- accept chat messages
- trigger icon generation behavior

Important backend files:
- `backend/src/main/java/com/equipe3/backend/BackendApplication.java`: Spring Boot entry point
- `backend/src/main/java/com/equipe3/backend/web/ProfileController.java`: REST endpoints
- `backend/src/main/java/com/equipe3/backend/service/`: business logic
- `backend/src/main/java/com/equipe3/backend/repository/`: profile storage layer

Default backend port:
- `8080`

## Prerequisites

### To run without Docker
Make sure you have:
- Java 21
- Node.js 20+ (recommended for the Angular 20 toolchain in this project)
- npm

### To run with Docker
Make sure you have:
- Docker
- Docker Compose plugin (`docker compose`)

## Run the project without Docker

You need **two terminals**: one for the backend and one for the frontend.

### 1) Start the backend

```bash
cd /home/bapsm/Documents/Equipe-3/backend
mvn spring-boot:run
```

The API should be available at:

```text
http://localhost:8080
```

### 2) Start the frontend

In another terminal:

```bash
cd /home/bapsm/Documents/Equipe-3/frontend
npm install
npm start
```

The Angular dev server should be available at:

```text
http://localhost:4200
```

### Local development notes

- The backend CORS configuration currently allows the Angular dev server at `http://localhost:4200`.
- The frontend uses `/api`, and the Angular dev server proxies that to `http://localhost:8080` via `frontend/proxy.conf.json`.
- The app currently loads a hard-coded player named `joueur1` on startup.

## Run the project with Docker

From the project root:

```bash
cd /home/bapsm/Documents/Equipe-3
docker compose up --build
```

This starts:
- `backend` on `localhost:8080`
- `frontend` on `localhost:4200`

Open the application here:

```text
http://localhost:4200
```

### Docker setup details

- `backend/Dockerfile` builds the Spring Boot jar with Maven and runs it with Java 21.
- `frontend/Dockerfile` builds the Angular app and serves the production files with Nginx.
- `frontend/nginx.conf` proxies `/api/*` requests to the backend container.
- `docker-compose.yml` starts both services on the same Docker network.

Because the frontend uses Nginx in Docker:
- browser requests go to `http://localhost:4200`
- API requests go to `/api/...`
- Nginx forwards them internally to `http://backend:8080/...`

## Stop the Docker stack

```bash
cd /home/bapsm/Documents/Equipe-3
docker compose down
```

## Useful development commands

### Backend

Run tests:

```bash
cd /home/bapsm/Documents/Equipe-3/backend
mvn test
```

Build the jar:

```bash
cd /home/bapsm/Documents/Equipe-3/backend
mvn clean package
```

### Frontend

Install dependencies:

```bash
cd /home/bapsm/Documents/Equipe-3/frontend
npm install
```

Run the dev server:

```bash
cd /home/bapsm/Documents/Equipe-3/frontend
npm start
```

Create a production build:

```bash
cd /home/bapsm/Documents/Equipe-3/frontend
npm run build
```

## Troubleshooting

### Frontend cannot reach the backend

If you run without Docker:
- make sure the backend is running on `http://localhost:8080`
- make sure the frontend is running on `http://localhost:4200`

If you run with Docker:
- make sure both containers started successfully
- restart with:

```bash
cd /home/bapsm/Documents/Equipe-3
docker compose up --build
```

### Port already in use

If a port is already taken on your machine, update `docker-compose.yml` or stop the conflicting process.

Current host ports used by Docker:
- `8080` for the backend
- `4200` for the frontend

### Maven wrapper note

The repository currently contains `backend/mvnw`, but the wrapper support files under `backend/.mvn/` are not present in the workspace. Because of that, the documented local backend commands use the system `mvn` command.

## Existing project docs

Additional files already present in the repository:
- `Design.md`
- `backend/HELP.md`
- `frontend/README.md`

