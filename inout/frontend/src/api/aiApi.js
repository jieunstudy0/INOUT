import client, { unwrap } from './apiClient';

/**
 * 답변 대기 문의에 대해 Gemini CS 분류·답변 초안을 즉시 생성합니다.
 * (백엔드 AiCsScheduler와 동일 로직의 수동 트리거)
 */
export function triggerAiCsClassification() {
  return unwrap(client.post('/admin/ai/cs-classify', null, { timeout: 40000 }));
}

/**
 * 재고·판매속도 기반 AI 발주 초안을 즉시 생성합니다.
 * (백엔드 AiAutoOrderScheduler와 동일 로직의 수동 트리거)
 */
export function triggerAiAutoOrderAnalysis() {
  return unwrap(client.post('/admin/ai/auto-order-analyze', null, { timeout: 40000 }));
}
