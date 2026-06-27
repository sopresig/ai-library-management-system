# Codex Handoff

## Project

Repository: https://github.com/sopresig/ai-library-management-system

Project name for report/PPT:

`基于 SpringCloud 微服务架构的 AI 智能图书管理系统`

This project was cloned from `thakurpdhiraj/library_management_system` and extended into a SpringCloud microservice library management system with an AI assistant.

## Current State

The project has been pushed to GitHub on branch `main`.

Latest local commits at handoff:

- `0dd0ff1` 合并远程仓库初始化文件
- `3886d55` 完成AI智能图书管理系统

Implemented work:

- Preserved the original library management features: login, admin/user views, book management, inventory, orders, user management.
- Added an AI assistant entry in the frontend.
- Added backend AI chat endpoint in `client-backend`.
- Added AI tool/RAG style logic for:
  - book recommendation
  - inventory lookup
  - borrowing policy Q&A
- Added OpenAI-compatible external model polishing.
- AI provider fallback order is:
  1. Xiaomi MiMo
  2. LM Studio on local machine
  3. built-in local rule/RAG answer
- Added 1000 fake books and matching inventory data.
- Added tests for AI controller, assistant service, and LLM fallback.

## Important Files

- `client-backend/src/main/java/com/dhitha/lms/clientbackend/controller/ClientAiController.java`
- `client-backend/src/main/java/com/dhitha/lms/clientbackend/service/LibraryAiAssistantService.java`
- `client-backend/src/main/java/com/dhitha/lms/clientbackend/service/ExternalLlmClient.java`
- `client-backend/src/main/java/com/dhitha/lms/clientbackend/config/AiModelProperties.java`
- `client-frontend/src/components/common/LibraryAiAssistant.vue`
- `client-frontend/src/service/ai.js`
- `book/src/main/resources/data.sql`
- `inventory/src/main/resources/data.sql`
- `AI_RUN.md`
- `.env.example`
- `docker-compose.yml`

## Secrets

Do not commit real API keys or tokens.

`.env` is ignored by Git. Put real local secrets there or set them in the shell.

Safe example file:

- `.env.example`

Real key variables:

- `MIMO_API_KEY`
- `MIMO_BASE_URL`
- `MIMO_MODEL`
- `LMSTUDIO_BASE_URL`
- `LMSTUDIO_MODEL`
- `LMSTUDIO_ENABLED`

Current default LM Studio model:

`qwen3.5-27b`

Docker uses this URL to reach LM Studio on the Windows host:

`http://host.docker.internal:12340/v1`

If running `client-backend` directly outside Docker, use:

`http://localhost:12340/v1`

## Run With Docker

From repository root:

```powershell
docker compose down
docker compose build
docker compose up -d
```

Open:

- Frontend: http://localhost:8090
- Backend BFF: http://localhost:8086/lms
- Eureka: http://localhost:8761

Known login:

- username: `admin`
- password: `pass`

## Verify AI Endpoint

PowerShell example:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri 'http://localhost:8086/lms/login' `
  -Method Post `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body 'username=admin&password=pass' `
  -WebSession $session `
  -UseBasicParsing `
  -ErrorAction Stop | Out-Null

$payload = @{
  sessionId = 'handoff-check'
  message = 'Recommend several Computer Networks books from the library.'
} | ConvertTo-Json

$ai = Invoke-RestMethod -Uri 'http://localhost:8086/lms/ai/chat' `
  -Method Post `
  -ContentType 'application/json; charset=utf-8' `
  -Body $payload `
  -WebSession $session `
  -TimeoutSec 90

$ai.modelProvider
$ai.intent
$ai.toolCalls.Count
$ai.sources.Count
```

Expected provider depends on local configuration:

- `xiaomi-mimo` if MiMo key works
- `lm-studio` if MiMo is unavailable and LM Studio is running
- `local-rule-rag-demo` if both external providers are unavailable

## Tests

Maven is installed in the original machine at:

`E:\codex\fen bu shi  big zuo ye\tools\apache-maven-3.9.16`

On a new machine, install Maven or use any Maven 3.x.

Run focused backend AI tests:

```powershell
cd client-backend
mvn test
```

Previously verified:

- `client-backend` tests passed.
- Docker build for `lms-client-service` passed.
- Runtime `/lms/ai/chat` returned `provider=lm-studio`, `intent=BOOK_RECOMMENDATION`, `sources=1`, `toolCalls=1`.

Note: some older original service tests outside the AI work may be stale because the seed data was changed heavily.

## Git Notes

Remote is:

```powershell
git remote -v
```

Expected:

```text
origin  https://github.com/sopresig/ai-library-management-system.git (fetch)
origin  https://github.com/sopresig/ai-library-management-system.git (push)
```

Before future pushes, run a quick secret check. At minimum:

```powershell
git status --short
git diff --cached -- . ':(exclude)client-frontend/package-lock.json' | Select-String -Pattern 'sk-[A-Za-z0-9_-]{20,}|github_pat_|ghp_|gho_'
```

## Suggested Next Work

- Add a short Chinese project report or PPT based on the course requirements.
- Improve frontend text encoding if any garbled Chinese appears in original views.
- Add a README section for AI features and demo steps.
- Optionally add screenshots for the AI assistant, book list, admin view, and login page.
- If the target computer has LM Studio with a different model name, update `LMSTUDIO_MODEL` in `.env`.
