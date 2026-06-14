import request from './request'
import type { ApiResponse } from './request'

export interface AiSource {
  documentId?: number
  title?: string
  content?: string
  score?: number
}

export interface AiToolCall {
  toolName: string
  status: string
  requestParams?: string
  responseData?: string
  errorMessage?: string
}

export interface AiChatResponse {
  answer: string
  conversationId: number
  needConfirm: boolean
  sources: AiSource[]
  toolCalls: AiToolCall[]
}

export interface AiConversation {
  id: number
  userId: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface AiMessage {
  id: number
  conversationId: number
  userId?: number
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  metadata?: string
  createdAt: string
}

export interface AiKnowledgeDocument {
  id: number
  title: string
  fileId?: number
  fileName?: string
  filePath?: string
  sourceType: string
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
  chunkCount: number
  errorMessage?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export function sendAiMessage(data: { conversationId?: number; message: string; mode?: string }) {
  return request.post<any, ApiResponse<AiChatResponse>>('/ai/chat', data)
}

export function getAiConversations() {
  return request.get<any, ApiResponse<AiConversation[]>>('/ai/conversations')
}

export function getAiMessages(conversationId: number) {
  return request.get<any, ApiResponse<AiMessage[]>>(`/ai/conversations/${conversationId}/messages`)
}

export function deleteAiConversation(conversationId: number) {
  return request.delete<any, ApiResponse<void>>(`/ai/conversations/${conversationId}`)
}

export function uploadKnowledge(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResponse<AiKnowledgeDocument>>('/ai/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getKnowledgeList() {
  return request.get<any, ApiResponse<AiKnowledgeDocument[]>>('/ai/knowledge/list')
}

export function getKnowledgeDetail(id: number) {
  return request.get<any, ApiResponse<AiKnowledgeDocument>>(`/ai/knowledge/${id}`)
}

export function deleteKnowledge(id: number) {
  return request.delete<any, ApiResponse<void>>(`/ai/knowledge/${id}`)
}

export function rebuildKnowledge(id: number) {
  return request.post<any, ApiResponse<AiKnowledgeDocument>>(`/ai/knowledge/${id}/rebuild`)
}
