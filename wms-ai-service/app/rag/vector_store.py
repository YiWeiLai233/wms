import logging
import uuid

from qdrant_client import QdrantClient
from qdrant_client.http import models
from qdrant_client.http.exceptions import UnexpectedResponse

from app.config import get_settings

logger = logging.getLogger(__name__)

_client: QdrantClient | None = None
_collection_ensured = False


def get_client() -> QdrantClient:
    global _client
    if _client is None:
        settings = get_settings()
        logger.info("Connecting to Qdrant at %s", settings.qdrant_url)
        _client = QdrantClient(url=settings.qdrant_url)
    return _client


def ensure_collection(client: QdrantClient) -> None:
    """确保集合存在，只在首次调用时检查"""
    global _collection_ensured
    if _collection_ensured:
        return
    settings = get_settings()
    try:
        client.get_collection(settings.qdrant_collection)
        _collection_ensured = True
    except UnexpectedResponse as e:
        if e.status_code == 404:
            logger.info("Creating Qdrant collection: %s", settings.qdrant_collection)
            client.create_collection(
                collection_name=settings.qdrant_collection,
                vectors_config=models.VectorParams(size=settings.vector_size, distance=models.Distance.COSINE),
            )
            _collection_ensured = True
        else:
            logger.error("Failed to check Qdrant collection: %s", e)
            raise
    except Exception as e:
        logger.error("Failed to connect to Qdrant: %s", e)
        raise


def reset_client():
    """重置客户端连接，用于重连场景"""
    global _client, _collection_ensured
    _client = None
    _collection_ensured = False


def upsert_chunks(
    document_id: int,
    title: str,
    file_name: str,
    chunks: list[dict],
    vectors: list[list[float]],
) -> list[dict]:
    client = get_client()
    ensure_collection(client)
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
    ensure_collection(client)
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
