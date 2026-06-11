import client, { unwrap } from './apiClient';

export function getMyDepositHistory(page = 0, size = 10) {
  return unwrap(client.get('/emp/deposit', { params: { page, size } }));
}

export function chargeMyDeposit(data) {
  return unwrap(client.post('/deposit/charge', data));
}