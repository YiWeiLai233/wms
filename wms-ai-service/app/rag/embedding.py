import hashlib
import math

from app.config import get_settings

_model = None
_model_failed = False


def embed_texts(texts: list[str]) -> list[list[float]]:
    model = get_model()
    if model is not None:
        vectors = model.encode(texts, normalize_embeddings=True)
        return [vector.tolist() for vector in vectors]
    return [hash_embedding(text, get_settings().vector_size) for text in texts]


def get_model():
    global _model, _model_failed
    if _model is not None:
        return _model
    if _model_failed:
        return None
    try:
        from sentence_transformers import SentenceTransformer

        _model = SentenceTransformer(get_settings().embedding_model)
        return _model
    except Exception:
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
