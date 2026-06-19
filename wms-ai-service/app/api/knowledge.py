import json
import logging
from pathlib import Path

import requests
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

from app.config import get_settings
from app.rag.chain import build_answer
from app.rag.embedding import embed_texts
from app.rag.loader import load_document
from app.rag.splitter import split_text
from app.rag.vector_store import delete_document, upsert_chunks

logger = logging.getLogger(__name__)
router = APIRouter()


class IngestRequest(BaseModel):
    documentId: int
    title: str | None = None
    fileName: str
    filePath: str


def verify_token(token: str) -> None:
    settings = get_settings()
    if not settings.ai_service_token:
        raise HTTPException(status_code=503, detail="AI service not configured: AI_SERVICE_TOKEN is empty")
    if token != settings.ai_service_token:
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
        logger.exception("Ingestion failed for document %d", request.documentId)
        try:
            callback_ingestion(request.documentId, "FAILED", [], str(exc))
        except Exception as cb_exc:
            logger.warning("回调失败状态失败: %s", cb_exc)
        raise HTTPException(status_code=500, detail="Ingestion failed") from exc


@router.delete("/documents/{document_id}")
def delete(document_id: int, x_ai_service_token: str = Header(default="")) -> dict[str, str]:
    verify_token(x_ai_service_token)
    delete_document(document_id)
    return {"status": "SUCCESS"}


def resolve_upload_path(file_path: str) -> Path:
    """解析上传路径，防止路径遍历攻击"""
    settings = get_settings()
    root = Path(settings.wms_upload_root).resolve()
    clean_path = file_path.lstrip("/\\")
    # 如果 filePath 不包含 uploads/ 前缀，自动加上
    if not clean_path.startswith("uploads/") and not clean_path.startswith("uploads\\"):
        clean_path = f"uploads/{clean_path}"
    resolved = (root / clean_path).resolve()
    # 安全检查：确保解析后的路径仍在允许的目录内
    if not resolved.is_relative_to(root):
        raise HTTPException(status_code=400, detail="invalid file path: path traversal detected")
    return resolved


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
    resp = requests.post(
        url,
        json=payload,
        headers={"X-AI-Service-Token": settings.ai_service_token},
        timeout=60,
    )
    resp.raise_for_status()
    try:
        data = resp.json()
    except ValueError:
        return
    business_code = data.get("code") if isinstance(data, dict) else None
    if business_code is not None and str(business_code) != "200":
        message = data.get("message") or data.get("msg") or "unknown error"
        raise RuntimeError(f"Backend callback rejected ingestion: {message}")


@router.get("/preview-answer")
def preview_answer(question: str, x_ai_service_token: str = Header(default="")) -> dict:
    """预览问答结果，需要认证"""
    verify_token(x_ai_service_token)
    return build_answer(question)
