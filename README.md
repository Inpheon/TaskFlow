# TaskFlow

TaskFlow porządkuje projekty, zadania i notatki w jednym miejscu. Tablica kanban, terminy, priorytety, statystyki i sugestia kolejnego zadania ułatwiają ocenę postępu oraz wybór pracy wymagającej uwagi.

## Architektura

- Vue 3 SPA udostępniane przez Nginx
- REST API w Spring Boot
- PostgreSQL z migracjami Flyway
- Docker Compose uruchamiający bazę, backend i frontend

Nginx udostępnia pliki frontendu, obsługuje routing SPA i przekazuje żądania `/api` do backendu. Backend jest modularnym monolitem i odpowiada za uwierzytelnianie, uprawnienia oraz reguły domenowe.

## Uruchomienie

Wymagane są Docker i Docker Compose.

```bash
docker compose up --build
```

Adresy:

- aplikacja: http://localhost:5173
- kontrola stanu backendu: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html

Konto demonstracyjne:

- e-mail: `demo@taskflow.local`
- hasło: `demo1234`

Zatrzymanie aplikacji:

```bash
docker compose down
```

## Praca lokalna

Wymagane są Java 21, Node.js 22, npm oraz Docker.

Uruchomienie bazy:

```bash
docker compose up -d db
```

Uruchomienie backendu:

```bash
./gradlew :backend:bootRun
```

Uruchomienie frontendu:

```bash
cd frontend
npm install
npm run dev
```

## Weryfikacja

Testy backendu wymagają działającego Docker Engine:

```bash
./gradlew :backend:test
```

Sprawdzenie typów i zbudowanie frontendu:

```bash
cd frontend
npm install
npm run build
```

## API

Aktualną specyfikację OpenAPI udostępnia Swagger UI. Pomocniczy, rozwijany wraz z projektem opis znajduje się w [docs/api-contract.md](docs/api-contract.md).
