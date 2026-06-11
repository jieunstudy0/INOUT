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