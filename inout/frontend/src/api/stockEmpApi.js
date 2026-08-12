import client, { unwrap } from './apiClient';


export function getEmpStockList(params) {
  return unwrap(client.get('/emp/stocks', { params }));
}


export function getEmpStockDetail(itemId) {
  return unwrap(client.get(`/emp/stocks/${itemId}`));
}


export function useEmpStock(data) {
  return unwrap(client.post('/emp/stocks/use', data));
}

/** 스마트 발주 추천 (최근 판매 속도 + 안전재고 휴리스틱) */
export function getAiStockSuggestions(limit = 8) {
  return unwrap(client.get('/emp/stocks/ai-suggestions', { params: { limit } }));
}