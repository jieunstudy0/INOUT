import axios from 'axios';
import { Toast } from '../utils/toast';

const client = axios.create({
  baseURL: '/api', 
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token;
    }
    return config;
  },
  (error) => Promise.reject(error)
);


client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status ?? null;
    const message = error?.response?.data?.header?.message || error?.response?.data?.message || '오류 발생';


    if (status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      Toast.warning('세션이 만료되었습니다. 다시 로그인해 주세요.');
      window.location.href = '/login';
    } else if (status === 403) {
      Toast.error('접근 권한이 없습니다.');
    } else {
      Toast.error(message);
    }
    return Promise.reject({ status, message });
  }
);


export function unwrap(responsePromise) {
  return responsePromise.then((response) => {
    const payload = response?.data;
    if (!payload) throw new Error('응답 데이터가 비어 있습니다.');

    if (Object.prototype.hasOwnProperty.call(payload, 'body')) {
      const body = payload.body;
      return body == null ? [] : body;
    }
    if (Object.prototype.hasOwnProperty.call(payload, 'data')) {
      return payload.data != null ? payload.data : [];
    }
    return payload;
  });
}

export default client;
