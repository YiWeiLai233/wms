import hashlib
import logging
import math
import threading

from app.config import get_settings

logger = logging.getLogger(__name__)

_model = None
_model_failed = False
_model_lock = threading.Lock()


def embed_texts(texts: list[str]) -> list[list[float]]:
    model = get_model()
    if model is not None:
        vectors = model.encode(texts, normalize_embeddings=True)
        return [vector.tolist() for vector in vectors]
    # 注意：hash向量与真实向量不兼容，混合使用会导致检索结果混乱
    logger.warning("使用hash伪向量，检索质量将大幅下降。请检查embedding模型配置。")
    return [hash_embedding(text, get_settings().vector_size) for text in texts]


def get_model():
    global _model, _model_failed
    if _model is not None:
        return _model
    if _model_failed:
        return None
    # 线程安全的模型初始化
    with _model_lock:
        # 双重检查锁
        if _model is not None:
            return _model
        if _model_failed:
            return None
        try:
            from sentence_transformers import SentenceTransformer

            settings = get_settings()
            logger.info("Loading embedding model: %s", settings.embedding_model)
            _model = SentenceTransformer(settings.embedding_model)
            logger.info("Embedding model loaded successfully")
            return _model
        except Exception as exc:
            logger.error("Failed to load embedding model: %s", exc)
            _model_failed = True
            return None


def hash_embedding(text: str, size: int) -> list[float]:
    values: list[float] = []
    seed = text.encode("utf-8", errors="ignore")
    counter = 0
    while len(values) < size:
        digest = hashlib.sha256(seed + counter.to_bytes(4, "big")).digest()
        values.extend((byte / 127.5) - 1.0 for byte in digest)
        counter += 1
    vector = values[:size]
    norm = math.sqrt(sum(value * value for value in vector)) or 1.0
    return [value / norm for value in vector]
