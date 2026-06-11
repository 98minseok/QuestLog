# QuestLog

QuestLog 작업영역입니다.

## Repository

- GitHub: `git@github.com:98minseok/QuestLog.git`
- Local path: `C:/hermes/QuestLog`

## 구성

```text
apps/quest-log-fe   Vue 3 + Vite + Vuetify frontend
apps/quest-log-be   Spring Boot backend, port 8081
apps/quest-log-bff  Spring Boot BFF, port 8082
docker-compose.yml  PostgreSQL + Keycloak local dependencies
```

## 필요 도구

- Java 21
- Node.js / npm
- Docker Desktop, PostgreSQL/Keycloak 실행용

현재 확인된 로컬 도구:

```text
Java 21
Node 24
npm 11
Docker CLI installed
```

## 최초 설치 / 빌드

### Frontend

```bash
cd C:/hermes/QuestLog/apps/quest-log-fe
npm install
npm run build
```

### Backend

```bash
cd C:/hermes/QuestLog/apps/quest-log-be
./mvnw -DskipTests package
```

### BFF

```bash
cd C:/hermes/QuestLog/apps/quest-log-bff
./mvnw -DskipTests package
```

## 로컬 인프라

Docker Desktop이 실행 중이어야 합니다.

```bash
cd C:/hermes/QuestLog
docker compose up -d
```

구성:

- PostgreSQL: `localhost:5434`
  - DB: `questlog`
  - User: `questlog`
  - Password: `questlog`
- Keycloak: `http://localhost:18080`
  - Admin user: `admin`
  - Admin password: `admin`

## 앱 실행

### Backend

```bash
cd C:/hermes/QuestLog/apps/quest-log-be
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8081/actuator/health
```

### BFF

```bash
cd C:/hermes/QuestLog/apps/quest-log-bff
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8082/api/bff/backend-health
```

### Frontend

```bash
cd C:/hermes/QuestLog/apps/quest-log-fe
npm run dev
```

Vite 기본 URL은 보통 `http://localhost:5173` 입니다.

## 현재 Hermes 작업영역 구축 메모

- `@mdi/font`가 `src/main.ts`에서 import되고 있었지만 dependency에 없어 FE 빌드가 실패했습니다.
- `apps/quest-log-fe/package.json`에 `@mdi/font`를 추가해 FE 빌드를 통과시켰습니다.
- BE/BFF는 `./mvnw -DskipTests package` 빌드 통과했습니다.
- BFF 테스트는 통과했습니다.
- BE 테스트는 Testcontainers가 Docker daemon을 필요로 하는데 현재 Docker Desktop daemon이 뜨지 않아 실패했습니다.
