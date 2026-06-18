from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    qdrant_url: str = "http://localhost:6333"
    qdrant_collection: str = "wms_knowledge"
    vector_size: int = 512

    wms_backend_url: str = "http://localhost:8080"
    wms_upload_root: str = "./uploads"
    # 生产环境必须通过环境变量配置随机token，不设置默认值
    ai_service_token: str = ""

    llm_provider: str = "deepseek"
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"

    embedding_model: str = "BAAI/bge-small-zh-v1.5"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    # 启动时校验必要配置
    if not settings.ai_service_token:
        import warnings
        warnings.warn("AI_SERVICE_TOKEN 未配置，服务将拒绝所有请求。请在环境变量或 .env 文件中设置 AI_SERVICE_TOKEN。")
    return settings()
