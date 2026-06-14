from qdrant_client import QdrantClient
from qdrant_client.http import models
import uuid

from app.config import get_settings

_client: QdrantClient | None = None


def get_client() -> QdrantClient:
    global _client
    if _client is None:
        _client = QdrantClient(url=get_settings().qdrant_url)
    ensure_collection(_client)
    return _client


def ensure_collection(client: QdrantClient) -> None:
    settings = get_settings()
    try:
        client.get_collection(settings.qdrant_collection)
    except Exception:
        client.create_collection(
            collection_name=settings.qdrant_collection,
            vectors_config=models.VectorParams(size=settings.vector_size, distance=models.Distance.COSINE),
        )


def upsert_chunks(
    document_id: int,
    title: str,
    file_name: str,
    chunks: list[dict],
    vectors: list[list[float]],
) -> list[dict]:
    client = get_client()
    settings = get_settings()
    points = []
    stored_chunks = []
    for chunk, vector in zip(chunks, vectors):
        vector_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"wms:{document_id}:{chunk['chunkIndex']}"))
        payload = {
            "documentId": document_id,
            "title": title,
            "fileName": file_name,
            "chunkIndex": chunk["chunkIndex"],
            "content": chunk["content"],
        }
        points.append(models.PointStruct(id=vector_id, vector=vector, payload=payload))
        stored_chunks.append(
            {
                "chunkIndex": chunk["chunkIndex"],
                "content": chunk["content"],
                "vectorId": vector_id,
                "metadata": payload,
            }
        )
    if points:
        client.upsert(collection_name=settings.qdrant_collection, points=points)
    return stored_chunks


def delete_document(document_id: int) -> None:
    client = get_client()
    settings = get_settings()
    client.delete(
        collection_name=settings.qdrant_collection,
        points_selector=models.FilterSelector(
            filter=models.Filter(
                must=[
                    models.FieldCondition(
                        key="documentId",
                        match=models.MatchValue(value=document_id),
                    )
                ]
            )
        ),
    )
