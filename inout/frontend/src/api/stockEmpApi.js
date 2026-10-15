import client, { unwrap } from './apiClient';


export function getEmpStockList(params) {
  return unwrap(client.get('/emp/stocks', { errorContext: '상품 목록을 불러오지 못했습니다.', params }));
}


export function getEmpStockDetail(itemId) {
  return unwrap(client.get(`/emp/stocks/${itemId}`, { errorContext: '상품 정보를 불러오지 못했습니다.' }));
}


export function useEmpStock(data) {
  return unwrap(client.post('/emp/stocks/use', data));
}

/** 스마트 발주 추천 (최근 판매 속도 + 안전재고 휴리스틱) */
export function getAiStockSuggestions(limit = 8) {
  return unwrap(client.get('/emp/stocks/ai-suggestions', { errorContext: 'AI 추천 상품을 불러오지 못했습니다.', params: { limit } }));
}