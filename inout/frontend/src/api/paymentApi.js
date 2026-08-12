import client, { unwrap } from './apiClient';

export function payWithDeposit(orderId, amount) {
  return unwrap(client.post('/payment/deposit', { 
    orderId: orderId, 
    amount: amount 
  }));
}