import axios from 'axios';
import { Toast } from '../utils/toast';

const sharedConfig = {
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
};

async function parseApiError(error) {
  const status = error?.response?.status ?? null;
  let data = error?.response?.data;
  // responseType: 'blob'(파일 다운로드) 요청은 오류 본문도 Blob으로 오므로 JSON으로 풀어 header.message를 읽는다
  if (data instanceof Blob && data.type?.includes('application/json')) {
    try {
      data = JSON.parse(await data.text());
    } catch {
      data = null;
    }
  }
  const message =
    data?.header?.message
    || data?.message
    || (error?.response
      ? '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      : '서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.');
  // 검증 오류(@Valid 실패)일 때만 body가 [{ field, message }] 배열로 온다
  const errors = Array.isArray(data?.body) ? data.body : [];
  return { status, message, errors };
}

/** 서버 검증 오류 목록을 { 필드이름: 문구 } 형태로 바꾼다 (입력칸별 오류 표시용) */
export function toFieldErrors(errors = []) {
  return Object.fromEntries(errors.map((e) => [e.field, e.message]));
}

/**
 * 인증 필요 API — Authorization 주입 + 401 시 로그인 리다이렉트.
 * 실패 토스트는 이 인터셉터가 한 번만 띄운다. 화면의 catch에서는 토스트를 다시 띄우지 않고
 * 화면 상태 정리(목록 비우기, 모달 닫기, 입력칸 오류 표시)만 한다.
 */
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
  async (error) => {
    const { status, message, errors } = await parseApiError(error);

    if (status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      Toast.warning('세션이 만료되었습니다. 다시 로그인해 주세요.');
      window.location.href = '/login';
    } else if (status === 403) {
      Toast.error('접근 권한이 없습니다.');
    } else {
      // 5xx·네트워크 오류는 서버 문구가 일반적이므로, 요청에 붙인 화면 문맥(errorContext)을 앞에 덧붙인다.
      // 4xx는 서버 문구가 이미 구체적이라 그대로 쓰고, 검증 오류면 첫 번째 필드 오류를 보여 준다.
      const context = error?.config?.errorContext;
      const withContext = context && (status == null || status >= 500) ? `${context} ${message}` : message;
      Toast.error(errors[0]?.message || withContext);
    }
    return Promise.reject({ status, message, errors });
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
  async (error) => {
    const { status, message, errors } = await parseApiError(error);
    return Promise.reject({ status, message, errors });
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
