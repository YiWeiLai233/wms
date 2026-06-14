from app.rag.chain import build_answer


def answer_question(request: dict) -> dict:
    question = str(request.get("message") or "").strip()
    if not question:
        return {
            "answer": "请输入要咨询的问题。",
            "needConfirm": False,
            "sources": [],
            "toolCalls": [],
        }
    return build_answer(question)
