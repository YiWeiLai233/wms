import logging

from app.llm.llm_client import chat_completion
from app.rag.retriever import retrieve

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """你是易仓 WMS 智能助手。
你只能基于知识库检索内容和工具返回结果回答。
你不能编造订单、库存、出库、退货、快递、用户数据。
当用户询问实时业务数据时，必须调用对应工具。
当用户询问系统规则、操作流程、异常处理时，优先使用 RAG 知识库。
当工具返回空结果时，明确说明未查询到。
当工具调用失败时，明确说明失败原因。
当用户要求修改库存、出库、退货、订单状态时，不能直接执行，必须返回 needConfirm=true，并生成操作计划。
回答要简洁、准确，适合仓库人员理解。"""


WRITE_KEYWORDS = ("调整库存", "改库存", "创建出库", "确认出库", "创建退货", "确认退货", "删除订单", "修改订单")


def build_answer(question: str) -> dict:
    if any(keyword in question for keyword in WRITE_KEYWORDS):
        return {
            "answer": "该请求涉及高风险写操作，第一阶段不会自动执行。我可以先整理操作计划，待后续确认机制上线后再处理。",
            "needConfirm": True,
            "sources": [],
            "toolCalls": [],
        }

    sources = retrieve(question)
    if not sources:
        return {
            "answer": "知识库中暂未检索到相关内容。请先上传相关流程文档后再提问。",
            "needConfirm": False,
            "sources": [],
            "toolCalls": [{"toolName": "knowledge_retrieval", "status": "SUCCESS"}],
        }

    context = "\n\n".join(f"来源：{source.get('title')}\n{source.get('content')}" for source in sources)
    try:
        answer = chat_completion(SYSTEM_PROMPT, question, context)
    except Exception as exc:
        logger.exception("LLM 调用失败")
        return {
            "answer": "AI 服务暂时不可用，请稍后再试。",
            "needConfirm": False,
            "sources": sources,
            "toolCalls": [{"toolName": "knowledge_retrieval", "status": "SUCCESS"}, {"toolName": "llm_completion", "status": "FAILED"}],
        }
    return {
        "answer": answer,
        "needConfirm": False,
        "sources": sources,
        "toolCalls": [{"toolName": "knowledge_retrieval", "status": "SUCCESS"}],
    }
