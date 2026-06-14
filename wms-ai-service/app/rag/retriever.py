from qdrant_client.http import models

from app.config import get_settings
from app.rag.embedding import embed_texts
from app.rag.vector_store import get_client


def retrieve(question: str, limit: int = 5) -> list[dict]:
    vector = embed_texts([question])[0]
    client = get_client()
    settings = get_settings()
    results = client.search(
        collection_name=settings.qdrant_collection,
        query_vector=vector,
        limit=limit,
        with_payload=True,
    )
    sources: list[dict] = []
    for item in results:
        payload = item.payload or {}
        sources.append(
            {
                "documentId": payload.get("documentId"),
                "title": payload.get("title"),
                "content": payload.get("content"),
                "score": item.score,
            }
        )
    return sources
