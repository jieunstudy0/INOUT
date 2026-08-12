import client, { unwrap } from './apiClient';

/**
 * AI 초안이 없는 문의를 최대 10건 일괄 처리합니다. (스케줄러 수동 트리거)
 */
export function triggerAiCsClassification() {
  return unwrap(client.post('/admin/ai/cs-classify', null, { timeout: 40000 }));
}

/**
 * 특정 문의 ID 한 건에 대해 AI 분류·답변 초안을 생성합니다.
 */
export function triggerAiCsClassificationForOne(inquiryId) {
  return unwrap(client.post(`/admin/ai/cs-classify/${inquiryId}`, null, { timeout: 40000 }));
}

/**
 * 재고·판매속도 기반 AI 발주 초안을 즉시 생성합니다.
 * (백엔드 AiAutoOrderScheduler와 동일 로직의 수동 트리거)
 */
export function triggerAiAutoOrderAnalysis() {
  return unwrap(client.post('/admin/ai/auto-order-analyze', null, { timeout: 40000 }));
}
