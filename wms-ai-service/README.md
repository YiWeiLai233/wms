# WMS AI Service

FastAPI service for the first-stage WMS AI assistant.

It handles knowledge document parsing, chunking, embeddings, Qdrant storage, and knowledge-base answers. It does not connect to MySQL and does not execute inventory, order, outbound, or return write operations.

## Run

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8010
```

Or from the repository root:

```bash
docker compose up qdrant wms-ai-service
```

## Environment

- `QDRANT_URL`: Qdrant endpoint, default `http://localhost:6333`
- `QDRANT_COLLECTION`: collection name, default `wms_knowledge`
- `WMS_BACKEND_URL`: Spring Boot backend URL, default `http://localhost:8080`
- `WMS_UPLOAD_ROOT`: root directory shared with Spring Boot uploads, default `./uploads`
- `AI_SERVICE_TOKEN`: service token shared with Spring Boot
- `LLM_PROVIDER`: `deepseek` or `openai`
- `LLM_API_KEY`: optional LLM API key
- `LLM_BASE_URL`: OpenAI-compatible base URL
- `LLM_MODEL`: chat model name
- `EMBEDDING_MODEL`: sentence-transformers model
