import requests

from app.config import get_settings


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
    response = requests.post(
        url,
        json=payload,
        headers={"Authorization": f"Bearer {settings.llm_api_key}"},
        timeout=30,
    )
    response.raise_for_status()
    data = response.json()
    return data["choices"][0]["message"]["content"].strip()


def fallback_answer(context: str) -> str:
    compact = context.strip().replace("\n\n", "\n")
    if len(compact) > 900:
        compact = compact[:900] + "..."
    return f"根据知识库检索内容：\n{compact}"
