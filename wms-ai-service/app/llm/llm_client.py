import logging

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from app.config import get_settings

logger = logging.getLogger(__name__)

# 创建带重试机制的session
_session = None


def _get_session() -> requests.Session:
    global _session
    if _session is None:
        _session = requests.Session()
        retry = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["POST"],
        )
        adapter = HTTPAdapter(max_retries=retry)
        _session.mount("https://", adapter)
        _session.mount("http://", adapter)
    return _session


def chat_completion(system_prompt: str, question: str, context: str) -> str:
    settings = get_settings()
    if not settings.llm_api_key:
        return fallback_answer(context)

    url = f"{settings.llm_base_url.rstrip('/')}/v1/chat/completions"

    payload = {
        "model": settings.llm_model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {
                "role": "user",
                "content": f"请只基于以下知识库内容回答问题。\n\n知识库内容：\n{context}\n\n问题：{question}",
            },
        ],
        "temperature": 0.2,
    }
    try:
        session = _get_session()
        response = session.post(
            url,
            json=payload,
            headers={"Authorization": f"Bearer {settings.llm_api_key}"},
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
        # 防御性解析：处理可能的异常格式
        choices = data.get("choices")
        if not choices or not isinstance(choices, list):
            raise ValueError(f"LLM response missing choices: {data}")
        message = choices[0].get("message")
        if not message or not isinstance(message, dict):
            raise ValueError(f"LLM response missing message: {data}")
        content = message.get("content")
        if content is None:
            raise ValueError(f"LLM response content is None: {data}")
        return content.strip()
    except Exception as exc:
        logger.error("LLM call failed: %s", exc)
        raise


def fallback_answer(context: str) -> str:
    compact = context.strip().replace("\n\n", "\n")
    if len(compact) > 900:
        compact = compact[:900] + "..."
    return f"根据知识库检索内容：\n{compact}"
