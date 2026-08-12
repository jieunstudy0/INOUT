import client, { unwrap } from './apiClient';

export function getOwnerDepositHistory(page = 0, size = 10) {
  return unwrap(client.get('/owner/deposit', { params: { page, size } }));
}

export function requestOwnerCharge(data) {
  return unwrap(client.post('/owner/charges', data));
}

export function getOwnerChargeRequests() {
  return unwrap(client.get('/owner/charges'));
}
