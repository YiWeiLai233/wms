import json
from typing import Any

import requests

from app.config import get_settings


def query_order_by_no(
    order_no: str | None = None,
    platform_order_no: str | None = None,
    context: dict | None = None,
) -> tuple[dict, dict]:
    return _post_tool(
        "/api/ai/tools/order/query",
        {"orderNo": order_no, "platformOrderNo": platform_order_no},
        context,
        "query_order_by_no",
    )


def search_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _post_tool("/api/ai/tools/order/search", payload or {}, context, "search_orders")


def count_pending_outbound_orders(context: dict | None = None) -> tuple[dict, dict]:
    return _post_tool(
        "/api/ai/tools/order/count-pending-outbound",
        {},
        context,
        "count_pending_outbound_orders",
    )


def query_sku_inventory(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _post_tool("/api/ai/tools/stock/query", payload or {}, context, "query_sku_inventory")


def query_outbound_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _post_tool("/api/ai/tools/outbound/query", payload or {}, context, "query_outbound_orders")


def query_return_orders(payload: dict | None = None, context: dict | None = None) -> tuple[dict, dict]:
    return _post_tool("/api/ai/tools/return/query", payload or {}, context, "query_return_orders")


def _post_tool(endpoint: str, payload: dict, context: dict | None, tool_name: str) -> tuple[dict, dict]:
    settings = get_settings()
    url = f"{settings.wms_backend_url.rstrip('/')}{endpoint}"
    request_payload = _clean_dict(payload)
    try:
        response = requests.post(
            url,
            json=request_payload,
            headers=_headers(context),
            timeout=10,
        )
        response.raise_for_status()
        body = response.json()
        if body.get("code") != 200:
            error = body.get("message") or "WMS backend returned an error"
            result = _failed_result(tool_name, error)
        else:
            result = body.get("data") or _failed_result(tool_name, "WMS backend returned empty data")
    except requests.RequestException as exc:
        result = _failed_result(tool_name, str(exc))
    except ValueError as exc:
        result = _failed_result(tool_name, f"Invalid WMS backend response: {exc}")

    return result, _tool_call(tool_name, request_payload, result)


def _headers(context: dict | None) -> dict:
    settings = get_settings()
    headers = {
        "Content-Type": "application/json",
        "X-AI-Service-Token": settings.ai_service_token,
    }
    context = context or {}
    header_map = {
        "userId": "X-AI-User-Id",
        "conversationId": "X-AI-Conversation-Id",
        "messageId": "X-AI-Message-Id",
    }
    for key, header in header_map.items():
        value = context.get(key)
        if value is not None:
            headers[header] = str(value)
    return headers


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
