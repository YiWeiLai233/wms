from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.api.knowledge import router as knowledge_router

app = FastAPI(title="WMS AI Service", version="0.1.0")

app.include_router(chat_router, prefix="/api")
app.include_router(knowledge_router, prefix="/api/knowledge")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
