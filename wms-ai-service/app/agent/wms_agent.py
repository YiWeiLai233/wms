import re
from typing import Any

from app.rag.chain import build_answer
from app.tools import wms_tools


WRITE_KEYWORDS = (
    "调整库存",
    "修改库存",
    "增加库存",
    "扣减库存",
    "创建出库",
    "生成出库",
    "确认出库",
    "取消出库",
    "创建退货",
    "确认退货",
    "取消退货",
    "质检通过",
    "修改订单",
    "取消订单",
    "删除订单",
    "改状态",
)

KNOWLEDGE_KEYWORDS = (
    "流程",
    "规则",
    "制度",
    "规范",
    "操作手册",
    "怎么处理",
    "如何处理",
    "异常",
    "说明",
)

ORDER_STATUS_MAP = {
    "待付款": "WAIT_PAY",
    "待出库": "WAIT_OUTBOUND",
    "未出库": "WAIT_OUTBOUND",
    "没出库": "WAIT_OUTBOUND",
    "出库中": "OUTBOUNDING",
    "已发货": "SHIPPED",
    "已完成": "FINISHED",
    "退货中": "RETURNING",
    "已退货": "RETURNED",
    "已取消": "CANCELLED",
    "取消": "CANCELLED",
}

OUTBOUND_STATUS_MAP = {
    "待拣货": "WAIT_PICKING",
    "拣货中": "PICKING",
    "已发货": "SHIPPED",
    "已取消": "CANCELLED",
}

RETURN_STATUS_MAP = {
    "待质检": "PENDING_CHECK",
    "可售": "SELLABLE",
    "次品": "DEFECTIVE",
    "报废": "SCRAPPED",
    "已完成": "COMPLETED",
    "已取消": "CANCELLED",
}


def answer_question(request: dict) -> dict:
    question = str(request.get("message") or "").strip()
    if not question:
        return {
            "answer": "请输入要咨询的问题。",
            "needConfirm": False,
            "sources": [],
            "toolCalls": [],
        }

    if _is_write_request(question):
        return {
            "answer": "该请求涉及写操作。第二阶段只支持只读查询，我不会执行库存、订单、出库或退货修改。后续第三阶段会进入确认机制。",
            "needConfirm": True,
            "sources": [],
            "toolCalls": [],
        }

    context = {
        "userId": request.get("userId"),
        "conversationId": request.get("conversationId"),
        "messageId": request.get("messageId"),
    }

    if _should_use_knowledge(question):
        return build_answer(question)

    routed = _route_readonly_tool(question, context)
    if routed is not None:
        return routed

    return build_answer(question)


def _route_readonly_tool(question: str, context: dict) -> dict | None:
    if _is_pending_outbound_count(question):
        result, tool_call = wms_tools.count_pending_outbound_orders(context)
        return _tool_response(result, tool_call, _format_pending_outbound_count)

    if _is_return_question(question):
        payload = _return_payload(question)
        result, tool_call = wms_tools.query_return_orders(payload, context)
        return _tool_response(result, tool_call, _format_return_result)

    if _is_outbound_question(question):
        payload = _outbound_payload(question)
        result, tool_call = wms_tools.query_outbound_orders(payload, context)
        return _tool_response(result, tool_call, _format_outbound_result)

    if _is_stock_question(question):
        payload = _stock_payload(question)
        result, tool_call = wms_tools.query_sku_inventory(payload, context)
        return _tool_response(result, tool_call, _format_stock_result)

    if _is_order_question(question):
        order_no = _extract_prefixed_no(question, "SO")
        platform_order_no = _extract_after_label(question, ("平台订单号", "平台单号"))
        if order_no or platform_order_no:
            result, tool_call = wms_tools.query_order_by_no(order_no, platform_order_no, context)
            return _tool_response(result, tool_call, _format_order_detail)

        result, tool_call = wms_tools.search_orders(_order_search_payload(question), context)
        return _tool_response(result, tool_call, _format_order_list)

    return None


def _tool_response(result: dict, tool_call: dict, formatter) -> dict:
    if result.get("status") == "FAILED":
        answer = f"工具调用失败：{result.get('errorMessage') or '未知错误'}"
    else:
        answer = formatter(result.get("data"))
    return {
        "answer": answer,
        "needConfirm": False,
        "sources": [],
        "toolCalls": [tool_call],
    }


def _is_write_request(question: str) -> bool:
    return any(keyword in question for keyword in WRITE_KEYWORDS)


def _should_use_knowledge(question: str) -> bool:
    if not _contains_any(question, KNOWLEDGE_KEYWORDS):
        return False
    read_signals = ("查询", "查看", "看下", "多少", "数量", "状态", "订单号", "单号", "SKU", "sku")
    return not _contains_any(question, read_signals) and not any(
        _extract_prefixed_no(question, prefix) for prefix in ("SO", "OB", "RT", "SKU")
    )


def _is_pending_outbound_count(question: str) -> bool:
    return any(word in question for word in ("还有多少没出库", "多少未出库", "待出库数量", "待出库订单数"))


def _is_order_question(question: str) -> bool:
    if _contains_any(question, ("订单", "平台单号", "平台订单号", "收件人", "手机号", "电话")):
        return True
    return _extract_prefixed_no(question, "SO") is not None


def _is_stock_question(question: str) -> bool:
    return _contains_any(question, ("库存", "SKU", "sku", "低库存", "缺货", "仓库"))


def _is_outbound_question(question: str) -> bool:
    return _contains_any(question, ("出库单", "出库状态", "拣货", "快递单号")) or _extract_prefixed_no(question, "OB") is not None


def _is_return_question(question: str) -> bool:
    return _contains_any(question, ("退货", "质检", "可售", "次品", "报废")) or _extract_prefixed_no(question, "RT") is not None


def _contains_any(text: str, words: tuple[str, ...]) -> bool:
    return any(word in text for word in words)


def _order_search_payload(question: str) -> dict:
    payload: dict[str, Any] = {"page": 1, "size": 10}
    phone = _extract_phone(question)
    if phone:
        payload["receiverPhone"] = phone
    status = _map_status(question, ORDER_STATUS_MAP)
    if status:
        payload["orderStatus"] = status
    keyword = _clean_keyword(question)
    if keyword and not phone:
        payload["keyword"] = keyword
    return payload


def _stock_payload(question: str) -> dict:
    payload: dict[str, Any] = {"page": 1, "size": 10}
    sku_code = _extract_prefixed_no(question, "SKU")
    if sku_code:
        payload["skuCode"] = sku_code
    elif "低库存" in question:
        payload["stockType"] = "low"
    elif "缺货" in question:
        payload["stockType"] = "out"
    else:
        product_name = _extract_between(question, ("查询", "看下", "查看"), ("库存", "的库存"))
        if product_name:
            payload["productName"] = product_name
    return payload


def _outbound_payload(question: str) -> dict:
    payload: dict[str, Any] = {"page": 1, "size": 10}
    outbound_no = _extract_prefixed_no(question, "OB")
    order_no = _extract_prefixed_no(question, "SO")
    if outbound_no:
        payload["outboundNo"] = outbound_no
    if order_no:
        payload["orderNo"] = order_no
    tracking_no = _extract_after_label(question, ("快递单号", "运单号"))
    if tracking_no:
        payload["trackingNo"] = tracking_no
    status = _map_status(question, OUTBOUND_STATUS_MAP)
    if status:
        payload["status"] = status
    return payload


def _return_payload(question: str) -> dict:
    payload: dict[str, Any] = {"page": 1, "size": 10}
    return_no = _extract_prefixed_no(question, "RT")
    order_no = _extract_prefixed_no(question, "SO")
    if return_no:
        payload["returnNo"] = return_no
    if order_no:
        payload["orderNo"] = order_no
    status = _map_status(question, RETURN_STATUS_MAP)
    if status:
        payload["status"] = status
    return payload


def _format_pending_outbound_count(data: Any) -> str:
    count = (data or {}).get("count", 0)
    return f"当前待出库订单数量：{count}。"


def _format_order_detail(data: Any) -> str:
    if not data:
        return "未查询到匹配的订单。"
    return "\n".join(
        _compact_lines(
            [
                f"订单号：{data.get('orderNo')}",
                f"平台订单号：{data.get('platformOrderNo')}",
                f"状态：{data.get('orderStatus')}",
                f"仓库：{data.get('warehouseName')}",
                f"收件人：{data.get('receiverName')} {data.get('receiverPhone')}",
                f"金额：{data.get('totalAmount')}",
                f"备注：{data.get('remark')}",
            ]
        )
    )


def _format_order_list(data: Any) -> str:
    return _format_page(data, "订单", ("orderNo", "platformOrderNo", "orderStatus", "warehouseName"))


def _format_stock_result(data: Any) -> str:
    return _format_page(data, "库存", ("skuCode", "skuName", "warehouseName", "quantity", "lockedQty", "defectiveQty"))


def _format_outbound_result(data: Any) -> str:
    if _looks_like_detail(data, "outboundNo"):
        return "\n".join(
            _compact_lines(
                [
                    f"出库单号：{data.get('outboundNo')}",
                    f"订单号：{data.get('orderNo')}",
                    f"状态：{data.get('status')}",
                    f"仓库：{data.get('warehouseName')}",
                    f"快递单号：{data.get('trackingNo')}",
                ]
            )
        )
    return _format_page(data, "出库单", ("outboundNo", "orderNo", "status", "warehouseName", "trackingNo"))


def _format_return_result(data: Any) -> str:
    if _looks_like_detail(data, "returnNo"):
        return "\n".join(
            _compact_lines(
                [
                    f"退货单号：{data.get('returnNo')}",
                    f"订单号：{data.get('orderNo')}",
                    f"状态：{data.get('status')}",
                    f"原因：{data.get('reason')}",
                    f"仓库：{data.get('warehouseName')}",
                ]
            )
        )
    return _format_page(data, "退货单", ("returnNo", "orderNo", "status", "reason", "warehouseName"))


def _format_page(data: Any, label: str, fields: tuple[str, ...]) -> str:
    if not data:
        return f"未查询到匹配的{label}。"
    items = data.get("list") or []
    total = data.get("total") or len(items)
    if not items:
        return f"未查询到匹配的{label}。"
    lines = [f"共查询到 {total} 条{label}记录，前 {min(len(items), 5)} 条如下："]
    for item in items[:5]:
        values = [str(item.get(field)) for field in fields if item.get(field) not in (None, "")]
        lines.append(" / ".join(values))
    return "\n".join(lines)


def _looks_like_detail(data: Any, key: str) -> bool:
    return isinstance(data, dict) and key in data and "list" not in data


def _compact_lines(lines: list[str]) -> list[str]:
    return [line for line in lines if not line.endswith("：None") and not line.endswith("：")]


def _extract_prefixed_no(text: str, prefix: str) -> str | None:
    match = re.search(rf"\b{prefix}[A-Za-z0-9_-]+\b", text, flags=re.IGNORECASE)
    return match.group(0) if match else None


def _extract_phone(text: str) -> str | None:
    match = re.search(r"\b1[3-9]\d{9}\b|\b\d{7,}\b", text)
    return match.group(0) if match else None


def _extract_after_label(text: str, labels: tuple[str, ...]) -> str | None:
    for label in labels:
        match = re.search(rf"{label}[：:\s]*([A-Za-z0-9_-]+)", text, flags=re.IGNORECASE)
        if match:
            return match.group(1)
    return None


def _extract_between(text: str, starts: tuple[str, ...], ends: tuple[str, ...]) -> str | None:
    for start in starts:
        for end in ends:
            match = re.search(rf"{start}(.+?){end}", text)
            if match:
                value = match.group(1).strip(" ，,。?")
                if value:
                    return value
    return None


def _map_status(text: str, status_map: dict[str, str]) -> str | None:
    for keyword, status in status_map.items():
        if keyword in text:
            return status
    return None


def _clean_keyword(text: str) -> str:
    value = text
    for word in KNOWLEDGE_KEYWORDS:
        value = value.replace(word, "")
    for word in ("查询", "查看", "看下", "帮我", "一下", "订单", "的"):
        value = value.replace(word, "")
    return value.strip(" ，,。?")
