# Kontrakt API

DOKUMENT ROBOCZY - bedzie ewoluowal razem z implementacja.
Szczegoly requestow, response'ow i walidacji moga sie zmieniac w trakcie pracy nad projektem.

Dokument opisuje planowany i implementowany kontrakt API. Zrodlem prawdy do gotowych endpointow jest swagger-ui, mozna go wykorzystac do testowania aktualnie dzialajacego API:

```text
http://localhost:8080/swagger-ui/index.html
```

Base URL:

```text
http://localhost:8080
```

Prefix API:

```text
/api
```

Autoryzacja dla endpointow chronionych:

```text
Authorization: Bearer <token>
```

## Health

### GET /api/health

Response 200:

```json
{
  "status": "UP",
  "service": "taskflow-backend",
  "timestamp": "2026-05-25T18:00:00Z"
}
```

## Auth

### POST /api/auth/register

Request:

```json
{
  "email": "demo@example.com",
  "password": "demo1234",
  "displayName": "Demo User"
}
```

Response 201:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "demo@example.com",
    "displayName": "Demo User"
  }
}
```

### POST /api/auth/login

Request:

```json
{
  "email": "demo@example.com",
  "password": "demo1234"
}
```

Response 200:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "demo@example.com",
    "displayName": "Demo User"
  }
}
```

### GET /api/auth/me

Response 200:

```json
{
  "id": "uuid",
  "email": "demo@example.com",
  "displayName": "Demo User"
}
```

## Projects

Endpointy wymagaja autoryzacji. Uzytkownik widzi tylko swoje projekty. Brak projektu i projekt nalezacy do innego uzytkownika zwracaja 404.

### GET /api/projects

Response 200:

```json
[
  {
    "id": "uuid",
    "name": "PAI Project",
    "description": "Architecture-first app",
    "createdAt": "2026-05-25T18:00:00Z",
    "updatedAt": "2026-05-25T18:00:00Z"
  }
]
```

### POST /api/projects

Request:

```json
{
  "name": "PAI Project",
  "description": "Architecture-first app"
}
```

Response 201:

```json
{
  "id": "uuid",
  "name": "PAI Project",
  "description": "Architecture-first app",
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:00:00Z"
}
```

### GET /api/projects/{projectId}

Response 200:

```json
{
  "id": "uuid",
  "name": "PAI Project",
  "description": "Architecture-first app",
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:00:00Z"
}
```

### PUT /api/projects/{projectId}

Request:

```json
{
  "name": "Updated name",
  "description": "Updated description"
}
```

Response 200:

```json
{
  "id": "uuid",
  "name": "Updated name",
  "description": "Updated description",
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:10:00Z"
}
```

### DELETE /api/projects/{projectId}

Response 204.

## Tasks

### GET /api/projects/{projectId}/tasks

Optional query params:

```text
status=TODO
priority=HIGH
```

Response 200:

```json
[
  {
    "id": "uuid",
    "projectId": "uuid",
    "title": "Prepare ADR",
    "description": "Write architecture decisions",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2026-05-30",
    "position": 0,
    "createdAt": "2026-05-25T18:00:00Z",
    "updatedAt": "2026-05-25T18:00:00Z",
    "completedAt": null
  }
]
```

### POST /api/projects/{projectId}/tasks

Request:

```json
{
  "title": "Prepare ADR",
  "description": "Write architecture decisions",
  "priority": "HIGH",
  "dueDate": "2026-05-30"
}
```

Response 201:

```json
{
  "id": "uuid",
  "projectId": "uuid",
  "title": "Prepare ADR",
  "description": "Write architecture decisions",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-05-30",
  "position": 0,
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:00:00Z",
  "completedAt": null
}
```

### GET /api/tasks/{taskId}

Response 200:

```json
{
  "id": "uuid",
  "projectId": "uuid",
  "title": "Prepare ADR",
  "description": "Write architecture decisions",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-05-30",
  "position": 0,
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:00:00Z",
  "completedAt": null
}
```

### PUT /api/tasks/{taskId}

Request:

```json
{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "MEDIUM",
  "dueDate": "2026-06-01"
}
```

Response 200:

```json
{
  "id": "uuid",
  "projectId": "uuid",
  "title": "Updated title",
  "description": "Updated description",
  "status": "TODO",
  "priority": "MEDIUM",
  "dueDate": "2026-06-01",
  "position": 0,
  "createdAt": "2026-05-25T18:00:00Z",
  "updatedAt": "2026-05-25T18:10:00Z",
  "completedAt": null
}
```

### DELETE /api/tasks/{taskId}

Response 204.

## Board i use-case endpoints

### GET /api/projects/{projectId}/board

Response 200:

```json
{
  "project": {
    "id": "uuid",
    "name": "PAI Project"
  },
  "columns": {
    "TODO": [],
    "IN_PROGRESS": [],
    "DONE": []
  }
}
```

### PATCH /api/tasks/{taskId}/move

Request:

```json
{
  "targetStatus": "IN_PROGRESS",
  "position": 1
}
```

Response 200:

```json
{
  "id": "uuid",
  "projectId": "uuid",
  "title": "Prepare ADR",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2026-05-30",
  "position": 1,
  "completedAt": null
}
```

### GET /api/projects/{projectId}/stats

Response 200:

```json
{
  "totalTasks": 12,
  "todo": 5,
  "inProgress": 4,
  "done": 3,
  "completionPercentage": 25
}
```

### GET /api/dashboard/summary

Response 200:

```json
{
  "projectsCount": 3,
  "openTasksCount": 14,
  "doneTasksCount": 7,
  "overdueTasksCount": 2,
  "highPriorityOpenTasksCount": 4
}
```

### GET /api/projects/{projectId}/report

Response 200:

```json
{
  "projectId": "uuid",
  "projectName": "PAI Project",
  "generatedAt": "2026-05-25T18:00:00Z",
  "totalTasks": 18,
  "doneTasks": 7,
  "inProgressTasks": 5,
  "todoTasks": 6,
  "completionPercentage": 39,
  "overdueTasks": 2,
  "highPriorityOpenTasks": 3
}
```

### GET /api/projects/{projectId}/suggested-next-task

Response 200:

```json
{
  "task": {
    "id": "uuid",
    "title": "Finish diagrams",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2026-05-25"
  },
  "reason": "High priority task with nearest due date"
}
```

## Notes

### GET /api/tasks/{taskId}/notes

Response 200:

```json
[
  {
    "id": "uuid",
    "taskId": "uuid",
    "authorId": "uuid",
    "content": "Remember to update ADR after implementation.",
    "createdAt": "2026-05-25T18:00:00Z"
  }
]
```

### POST /api/tasks/{taskId}/notes

Request:

```json
{
  "content": "Remember to update ADR after implementation."
}
```

Response 201:

```json
{
  "id": "uuid",
  "taskId": "uuid",
  "authorId": "uuid",
  "content": "Remember to update ADR after implementation.",
  "createdAt": "2026-05-25T18:00:00Z"
}
```

### DELETE /api/notes/{noteId}

Response 204.

## Error response

```json
{
  "status": 400,
  "error": "Validation error",
  "message": "Task title must not be blank",
  "path": "/api/projects/{projectId}/tasks",
  "timestamp": "2026-05-25T18:00:00Z"
}
```
