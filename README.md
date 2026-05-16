# Personal Task Manager

A small Java 17 Spring Boot REST API for managing personal tasks, with an H2 in-memory database, a minimal browser UI, and an AI-powered task suggestion endpoint.

## Requirements

- Java 17 or newer
- Internet access on first run so Maven can download dependencies

No external database or frontend build tools are required.

## Run Locally

Start the API and UI with one command:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080/
```

The static UI supports:

- Viewing tasks
- Creating tasks
- Asking the AI endpoint for a task suggestion
- Creating a task from the AI suggestion

## Run Tests

```bash
./mvnw test
```

The test suite covers:

- Service-layer happy paths for task CRUD behavior
- End-to-end CRUD endpoint behavior
- AI suggestion endpoint behavior with a mocked `AiClient`

## Task API

`Task` fields:

- `id`: auto-generated
- `title`: required string
- `description`: optional string
- `dueDate`: optional ISO date, for example `2026-05-22`
- `priority`: `LOW`, `MEDIUM`, or `HIGH`
- `status`: `TODO`, `IN_PROGRESS`, or `DONE`

Endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/tasks` | Create a task |
| `GET` | `/tasks` | List all tasks |
| `GET` | `/tasks/{id}` | Get one task |
| `PUT` | `/tasks/{id}` | Update a task |
| `DELETE` | `/tasks/{id}` | Delete a task |

Create example:

```bash
curl -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Submit quarterly report",
    "description": "Send the final report to finance",
    "dueDate": "2026-05-22",
    "priority": "HIGH",
    "status": "TODO"
  }'
```

Example response:

```json
{
  "id": 1,
  "title": "Submit quarterly report",
  "description": "Send the final report to finance",
  "dueDate": "2026-05-22",
  "priority": "HIGH",
  "status": "TODO",
  "createdAt": "2026-05-16T13:30:00.000Z",
  "updatedAt": "2026-05-16T13:30:00.000Z"
}
```

If `priority` is omitted on create, it defaults to `MEDIUM`. If `status` is omitted on create, it defaults to `TODO`.

## AI Task Suggestion Endpoint

Endpoint:

```text
POST /tasks/suggest
```

This endpoint accepts a plain-language task description and returns a structured task suggestion. It does not persist the task. Use `POST /tasks` to create a task from the returned suggestion.

Example request:

```bash
curl -X POST http://localhost:8080/tasks/suggest \
  -H 'Content-Type: application/json' \
  -d '{
    "description": "remind me to submit the quarterly report before Friday"
  }'
```

Example response:

```json
{
  "title": "Submit quarterly report",
  "description": "Submit the quarterly report before Friday.",
  "dueDate": "2026-05-22",
  "priority": "MEDIUM",
  "status": "TODO"
}
```

### AI Configuration

The AI integration uses OpenAI's Responses API through `OpenAiClient`.

Set these environment variables before starting the app:

```bash
export OPENAI_API_KEY="your-api-key"
export OPENAI_MODEL="gpt-5.4-mini"
./mvnw spring-boot:run
```

`OPENAI_MODEL` is optional. The app defaults to `gpt-5.4-mini`.

Timeouts are configured in `src/main/resources/application.properties`:

```properties
ai.openai.connect-timeout=5s
ai.openai.read-timeout=20s
```

If `OPENAI_API_KEY` is not configured, `/tasks/suggest` returns `503 Service Unavailable` with a JSON error response. CRUD endpoints still work normally.

## H2 Database Notes

The app uses an in-memory H2 database. Data resets every time the app restarts.

H2 console:

```text
http://localhost:8080/h2-console
```

Connection settings:

```text
JDBC URL: jdbc:h2:mem:taskmanager
User: sa
Password: <leave blank>
```

Schema creation is handled by Hibernate with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Error Format

Expected validation, not-found, bad-request, and AI availability failures are returned as JSON:

```json
{
  "timestamp": "2026-05-16T13:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "title: title is required",
  "path": "/tasks"
}
```
