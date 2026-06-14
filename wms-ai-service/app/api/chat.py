from pydantic import BaseModel, Field
from fastapi import APIRouter, Header, HTTPException

from app.agent.wms_agent import answer_question
from app.config import get_settings

router = APIRouter()


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    conversationId: int | None = None
    userId: int | None = None
    message: str
    mode: str = "knowledge"
    history: list[ChatMessage] = Field(default_factory=list)


@router.post("/chat")
def chat(request: ChatRequest, x_ai_service_token: str = Header(default="")) -> dict:
    settings = get_settings()
    if x_ai_service_token != settings.ai_service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")
    return answer_question(request.model_dump())
