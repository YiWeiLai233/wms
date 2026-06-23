"""MCP 工具封装 — 直接调用 MCP Server 获取数据，不依赖后端 API。"""

import json
import logging
from typing import Any

from app.tools import mcp_client

logger = logging.getLogger(__name__)


def query_order_by_no(
    order_no: str | None = None,
    platform_order_no: str | None = None,
    context: dict | None = None,
) -> tuple[dict, dict]:
    args = {}
    if order_no:
        args["order_no"] = order_no
    if platform_order_no:
        args["platform_order_no"] = platform_order_no
    return _call("query_order", args)


def search_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _call("search_orders", _snake(payload or {}))


def count_pending_outbound_orders(context: dict | None = None) -> tuple[dict, dict]:
    return _call("count_pending_outbound", {})


def query_sku_inventory(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _call("query_inventory", _snake(payload or {}))


def query_outbound_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _call("query_outbound", _snake(payload or {}))


def query_return_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _call("query_return", _snake(payload or {}))


def create_pending_action(payload: dict, context: dict | None = None) -> tuple[dict | None, dict]:
    context = context or {}
    args = _snake(dict(payload))
    if args.get("user_id") is None:
        args["user_id"] = context.get("userId")
    if args.get("conversation_id") is None:
        args["conversation_id"] = context.get("conversationId")

    result = mcp_client.call_tool("create_pending_action", _clean(args))
    if "error" in result:
        return None, _tool_log("create_pending_action", payload, "FAILED", result["error"])

    pending = result.get("data") if isinstance(result, dict) else result
    return pending, _tool_log("create_pending_action", payload, "SUCCESS")


# ── 内部工具 ──────────────────────────────────────────

def _call(mcp_name: str, args: dict) -> tuple[dict, dict]:
    result = mcp_client.call_tool(mcp_name, _clean(args))
    if "error" in result:
        return {"status": "FAILED", "data": None, "errorMessage": result["error"]}, _tool_log(mcp_name, args, "FAILED", result["error"])
    code = result.get("code")
    if code is not None and code != 200:
        msg = result.get("message") or "后端返回错误"
        return {"status": "FAILED", "data": None, "errorMessage": msg}, _tool_log(mcp_name, args, "FAILED", msg)
    data = result.get("data") if isinstance(result, dict) else result
    return {"status": "SUCCESS", "data": data}, _tool_log(mcp_name, args, "SUCCESS")


def _snake(d: dict) -> dict:
    import re
    return {re.sub(r"([A-Z])", r"_\1", k).lstrip("_").lower(): v for k, v in d.items()}


def _clean(d: dict) -> dict:
    return {k: v for k, v in d.items() if v is not None and v != ""}


def _tool_log(name: str, params: dict, status: str, error: str | None = None) -> dict:
    return {"toolName": name, "status": status, "requestParams": json.dumps(params, ensure_ascii=False, default=str), "errorMessage": error}
