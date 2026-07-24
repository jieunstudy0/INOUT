import client, { unwrap } from './apiClient';

/**
 * OpenAI 기반 발주/재고 운영 인사이트 리포트를 요청합니다.
 * AI 응답까지 최대 35초가 소요될 수 있으므로 timeout을 40초로 설정합니다.
 */
export function getAiInsightReport() {
  return unwrap(client.get('/admin/ai/insight', { timeout: 40000 }));
}

/**
 * 답변 대기 중인 고객 문의를 Gemini가 즉시 분류/초안 작성하도록 수동 트리거합니다.
 * 10분마다 자동 실행되는 스케줄러와 별개로, 관리자가 결과를 바로 확인하고 싶을 때 사용합니다.
 */
export function triggerAiCsClassification() {
  return unwrap(client.post('/admin/ai/cs-classify', null, { timeout: 40000 }));
}

/**
 * 전체 활성 상품의 재고·최근 7일 판매 속도를 Gemini가 즉시 분석하여
 * 재고 보충이 시급한 상품에 대한 발주 제안 초안을 생성하도록 수동 트리거합니다.
 * 매일 자정 자동 실행되는 스케줄러와 별개로, 관리자가 즉시 결과를 확인하고 싶을 때 사용합니다.
 */
export function triggerAiAutoOrderAnalysis() {
  return unwrap(client.post('/admin/ai/auto-order-analyze', null, { timeout: 40000 }));
}
