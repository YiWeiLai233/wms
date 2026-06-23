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
    tool_name = "query_order"
    args = {}
    if order_no:
        args["order_no"] = order_no
    if platform_order_no:
        args["platform_order_no"] = platform_order_no
    return _call_mcp_tool(tool_name, args, context, tool_name)


def search_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    tool_name = "search_orders"
    args = _snake_keys(payload or {})
    return _call_mcp_tool(tool_name, args, context, tool_name)


def count_pending_outbound_orders(context: dict | None = None) -> tuple[dict, dict]:
    tool_name = "count_pending_outbound"
    return _call_mcp_tool(tool_name, {}, context, tool_name)


def query_sku_inventory(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    tool_name = "query_inventory"
    args = _snake_keys(payload or {})
    return _call_mcp_tool(tool_name, args, context, tool_name)


def query_outbound_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    tool_name = "query_outbound"
    args = _snake_keys(payload or {})
    return _call_mcp_tool(tool_name, args, context, tool_name)


def query_return_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    tool_name = "query_return"
    args = _snake_keys(payload or {})
    return _call_mcp_tool(tool_name, args, context, tool_name)


def create_pending_action(payload: dict, context: dict | None = None) -> tuple[dict | None, dict]:
    tool_name = "create_pending_action"
    request_payload = dict(payload)
    context = context or {}
    if request_payload.get("userId") is None:
        request_payload["userId"] = context.get("userId")
    if request_payload.get("conversationId") is None:
        request_payload["conversationId"] = context.get("conversationId")

    args = _snake_keys(request_payload)
    result = mcp_client.call_tool(tool_name, args)

    if "error" in result:
        tool_call = _tool_call(tool_name, request_payload, _failed_result(tool_name, result["error"]))
        return None, tool_call

    pending_action = result.get("data")
    tool_result = {
        "toolName": tool_name,
        "status": "SUCCESS",
        "data": pending_action,
        "summary": pending_action.get("summary") if isinstance(pending_action, dict) else None,
        "errorMessage": None,
    }
    return pending_action, _tool_call(tool_name, request_payload, tool_result)


def _call_mcp_tool(mcp_name: str, args: dict, context: dict | None, tool_name: str) -> tuple[dict, dict]:
    request_payload = _clean_dict(args)
    result = mcp_client.call_tool(mcp_name, request_payload)

    if "error" in result:
        tool_result = _failed_result(tool_name, result["error"])
    else:
        # MCP Server 直接返回后端响应：{"code": 200, "data": ...}
        code = result.get("code")
        if code is not None and code != 200:
            error = result.get("message") or result.get("msg") or "后端返回错误"
            tool_result = _failed_result(tool_name, error)
        else:
            data = result.get("data") if isinstance(result, dict) else result
            tool_result = {
                "toolName": tool_name,
                "status": "SUCCESS",
                "data": data,
                "summary": None,
                "errorMessage": None,
            }
            if data is None:
                tool_result["status"] = "FAILED"
                tool_result["errorMessage"] = "MCP 工具返回空数据"

    return tool_result, _tool_call(tool_name, request_payload, tool_result)


def _snake_keys(d: dict) -> dict:
    """将驼峰 key 转为下划线 key（MCP 工具参数用下划线）。"""
    import re
    result = {}
    for k, v in d.items():
        snake = re.sub(r"([A-Z])", r"_\1", k).lstrip("_").lower()
        result[snake] = v
    return result


def _tool_call(tool_name: str, request_payload: dict, result: dict) -> dict:
    status = result.get("status") or "FAILED"
    return {
        "toolName": tool_name,
        "status": status,
        "requestParams": _to_json(request_payload),
        "responseData": _to_json(result),
        "errorMessage": result.get("errorMessage"),
    }


def _failed_result(tool_name: str, error: str) -> dict:
    return {
        "toolName": tool_name,
        "status": "FAILED",
        "data": None,
        "summary": None,
        "errorMessage": error,
    }


def _clean_dict(payload: dict) -> dict:
    return {key: value for key, value in payload.items() if value is not None and value != ""}


def _to_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, default=str)
