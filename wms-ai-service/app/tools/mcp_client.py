"""MCP 客户端 — 后台线程运行事件循环，提供同步调用接口。"""

import asyncio
import json
import logging
import threading
from concurrent.futures import Future

from mcp.client.session import ClientSession
from mcp.client.sse import sse_client

from app.config import get_settings

logger = logging.getLogger(__name__)


class McpClient:
    """MCP 客户端单例。在后台线程维护事件循环和连接，对外提供同步 call_tool 接口。"""

    _instance: "McpClient | None" = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    instance = super().__new__(cls)
                    instance._session: ClientSession | None = None
                    instance._loop: asyncio.AbstractEventLoop | None = None
                    instance._thread: threading.Thread | None = None
                    instance._ready = threading.Event()
                    cls._instance = instance
        return cls._instance

    def start(self):
        """启动后台线程和事件循环。"""
        if self._thread and self._thread.is_alive():
            return
        self._thread = threading.Thread(target=self._run_loop, daemon=True)
        self._thread.start()
        # 等待连接建立（最多 10 秒）
        self._ready.wait(timeout=10)

    def _run_loop(self):
        """后台线程：运行事件循环。"""
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        self._loop.run_until_complete(self._connect_and_run())

    async def _connect_and_run(self):
        """连接 MCP Server 并保持连接。"""
        settings = get_settings()
        url = settings.mcp_server_url
        logger.info("MCP 客户端连接: %s", url)
        try:
            async with sse_client(url) as (read, write):
                async with ClientSession(read, write) as session:
                    await session.initialize()
                    self._session = session
                    self._ready.set()
                    logger.info("MCP 客户端连接成功")
                    # 保持连接，直到进程退出
                    await asyncio.Event().wait()
        except Exception as exc:
            logger.exception("MCP 客户端连接失败: %s", exc)
            self._ready.set()  # 解除等待，避免死锁

    def call_tool(self, name: str, arguments: dict) -> dict:
        """同步调用 MCP 工具。"""
        if self._session is None:
            return {"error": "MCP 未连接"}

        if self._loop is None or self._loop.is_closed():
            return {"error": "MCP 事件循环未运行"}

        future: Future = asyncio.run_coroutine_threadsafe(
            self._call_tool_async(name, arguments), self._loop
        )
        try:
            return future.result(timeout=30)
        except Exception as exc:
            logger.exception("MCP 调用超时或异常: %s", name)
            return {"error": str(exc)}

    async def _call_tool_async(self, name: str, arguments: dict) -> dict:
        """异步调用 MCP 工具。"""
        try:
            result = await self._session.call_tool(name, arguments=arguments)
            if result.content and hasattr(result.content[0], "text"):
                return json.loads(result.content[0].text)
            return {"error": "MCP 工具返回空结果"}
        except Exception as exc:
            logger.exception("MCP 工具执行失败: %s", name)
            return {"error": str(exc)}


# 全局单例
_client: McpClient | None = None


def get_client() -> McpClient:
    """获取 MCP 客户端单例，首次调用时自动启动。"""
    global _client
    if _client is None:
        _client = McpClient()
        _client.start()
    return _client


def call_tool(name: str, arguments: dict) -> dict:
    """便捷函数：调用 MCP 工具。"""
    return get_client().call_tool(name, arguments)
