# TaskFlow

Minimalny szkielet aplikacji TaskFlow.

## Wymagania

- Docker + Docker Compose
- Java 21
- Node.js 22
- npm

## Uruchomienie całego środowiska

```bash
docker compose up --build
```

Adresy:

- frontend: http://localhost:5173
- backend health: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html

Zatrzymanie:

```bash
docker compose down
```

## Development

Backend:

```bash
./gradlew :backend:bootRun
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Testy

Backend:

```bash
./gradlew :backend:test
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

## Kontrakt API

Roboczy kontrakt API znajduje sie w [docs/api-contract.md](docs/api-contract.md).
