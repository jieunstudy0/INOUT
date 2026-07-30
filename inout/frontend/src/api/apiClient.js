import axios from 'axios';
import { Toast } from '../utils/toast';

const sharedConfig = {
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
};

function parseApiError(error) {
  const status = error?.response?.status ?? null;
  const message =
    error?.response?.data?.header?.message
    || error?.response?.data?.message
    || error?.message
    || '오류 발생';
  return { status, message };
}

/** 인증 필요 API — Authorization 주입 + 401 시 로그인 리다이렉트 */
const client = axios.create(sharedConfig);

client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const { status, message } = parseApiError(error);

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
  },
);

/**
 * 비인증(공개) API — login / find / password reset 등.
 * withCredentials 로 HttpOnly Refresh Token 쿠키를 크로스 도메인에서도 주고·전송한다.
 * Authorization 주입·401 리다이렉트는 하지 않는다.
 */
export const publicClient = axios.create(sharedConfig);

publicClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const { status, message } = parseApiError(error);
    return Promise.reject({ status, message });
  },
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
