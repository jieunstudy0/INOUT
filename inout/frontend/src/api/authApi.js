import axios from 'axios';
import client, { unwrap } from './apiClient';

export async function login(email, password) {
  const response = await axios.post('/api/user/login', { 
    email: email, 
    password: password 
  });
  return response?.data?.body ?? response?.data;
}


export function logout() {
  return unwrap(client.post('/user/logout'));
}