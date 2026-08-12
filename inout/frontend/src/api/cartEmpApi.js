import client, { unwrap } from './apiClient';

export function addToCart(itemId, quantity) {
  return unwrap(client.post('/emp/carts', { itemId, quantity }));
}