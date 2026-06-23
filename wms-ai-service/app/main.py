from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.api.knowledge import router as knowledge_router
from app.tools import mcp_client


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时初始化 MCP 客户端
    mcp_client.get_client()
    yield
    # 关闭时断开 MCP 连接
    # mcp_client 后台线程是 daemon，进程退出自动结束


app = FastAPI(title="WMS AI Service", version="0.1.0", lifespan=lifespan)

app.include_router(chat_router, prefix="/api")
app.include_router(knowledge_router, prefix="/api/knowledge")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
