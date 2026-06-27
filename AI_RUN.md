# Library AI System Run Notes

This project is a Spring Cloud + Vue library management system with an AI assistant added to the user and admin views.

## AI environment

Set the MiMo key in the current PowerShell session before starting Docker. Do not commit real keys.

```powershell
$env:MIMO_API_KEY="<your-mimo-api-key>"
$env:MIMO_BASE_URL="https://api.xiaomimimo.com/v1"
$env:MIMO_MODEL="mimo-v2.5-pro"
$env:LMSTUDIO_BASE_URL="http://host.docker.internal:12340/v1"
$env:LMSTUDIO_MODEL="qwen3.5-27b"
```

The assistant tries Xiaomi MiMo first, then LM Studio, then the built-in library RAG/tool layer. When running the backend in Docker, `host.docker.internal` lets the container reach LM Studio running on the Windows host. If you run the backend directly outside Docker, use `http://localhost:12340/v1`.

## Run with Docker

```powershell
cd "E:\codex\fen bu shi  big zuo ye\library-ai-system"
docker compose build --provenance=false --sbom=false
docker compose up -d --no-build
```

Open:

- Frontend: http://localhost:8090
- Client backend: http://localhost:8086/lms
- Eureka: http://localhost:8761

Default demo accounts:

- Admin: `admin` / `pass`
- User: `user` / `pass`

## AI features

- Business-integrated AI assistant in both user and admin pages.
- RAG-style policy answers from built-in borrowing, renewal, and reservation rules.
- Function Calling-style business tools for book search, inventory lookup, and recommendation.
- Optional Xiaomi MiMo OpenAI-compatible chat enrichment through `MIMO_API_KEY`.
- LM Studio fallback through `LMSTUDIO_BASE_URL` when MiMo is unavailable.
- AI trace panel showing intent, model provider, tool calls, and knowledge sources.
