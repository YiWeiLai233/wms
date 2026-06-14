from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    qdrant_url: str = "http://localhost:6333"
    qdrant_collection: str = "wms_knowledge"
    vector_size: int = 512

    wms_backend_url: str = "http://localhost:8080"
    wms_upload_root: str = "./uploads"
    ai_service_token: str = "ChangeMeAiServiceToken"

    llm_provider: str = "deepseek"
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"

    embedding_model: str = "BAAI/bge-small-zh-v1.5"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    return Settings()
