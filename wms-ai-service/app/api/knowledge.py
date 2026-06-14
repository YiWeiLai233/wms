import json
from pathlib import Path

import requests
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel

from app.config import get_settings
from app.rag.chain import build_answer
from app.rag.embedding import embed_texts
from app.rag.loader import load_document
from app.rag.splitter import split_text
from app.rag.vector_store import delete_document, upsert_chunks

router = APIRouter()


class IngestRequest(BaseModel):
    documentId: int
    title: str | None = None
    fileName: str
    filePath: str


def verify_token(token: str) -> None:
    if token != get_settings().ai_service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")


@router.post("/ingest")
def ingest(request: IngestRequest, x_ai_service_token: str = Header(default="")) -> dict[str, str]:
    verify_token(x_ai_service_token)
    try:
        path = resolve_upload_path(request.filePath)
        text = load_document(path)
        chunks = split_text(text)
        vectors = embed_texts([chunk["content"] for chunk in chunks])
        stored_chunks = upsert_chunks(
            document_id=request.documentId,
            title=request.title or request.fileName,
            file_name=request.fileName,
            chunks=chunks,
            vectors=vectors,
        )
        callback_ingestion(request.documentId, "SUCCESS", stored_chunks, None)
        return {"status": "SUCCESS"}
    except Exception as exc:
        callback_ingestion(request.documentId, "FAILED", [], str(exc))
        return {"status": "FAILED", "error": str(exc)}


@router.delete("/documents/{document_id}")
def delete(document_id: int, x_ai_service_token: str = Header(default="")) -> dict[str, str]:
    verify_token(x_ai_service_token)
    delete_document(document_id)
    return {"status": "SUCCESS"}


def resolve_upload_path(file_path: str) -> Path:
    settings = get_settings()
    clean_path = file_path.lstrip("/\\")
    return Path(settings.wms_upload_root) / clean_path


def callback_ingestion(document_id: int, status: str, chunks: list[dict], error: str | None) -> None:
    settings = get_settings()
    url = f"{settings.wms_backend_url.rstrip('/')}/api/ai/internal/knowledge/{document_id}/chunks"
    payload = {
        "status": status,
        "errorMessage": error,
        "chunks": [
            {
                "chunkIndex": chunk["chunkIndex"],
                "content": chunk["content"],
                "vectorId": chunk["vectorId"],
                "metadata": json.dumps(chunk.get("metadata", {}), ensure_ascii=False),
            }
            for chunk in chunks
        ],
    }
    requests.post(
        url,
        json=payload,
        headers={"X-AI-Service-Token": settings.ai_service_token},
        timeout=20,
    ).raise_for_status()


@router.get("/preview-answer")
def preview_answer(question: str) -> dict:
    return build_answer(question)
