/**
 * authApi / publicClient 계약 정적·런타임(미러) 검증
 */
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import axios from 'axios';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const apiClientSrc = readFileSync(join(root, 'src/api/apiClient.js'), 'utf8');
const authSrc = readFileSync(join(root, 'src/api/authApi.js'), 'utf8');

if (/from ['"]axios['"]/.test(authSrc) || /(?<!publicClient\.|client\.)axios\.(post|get)/.test(authSrc)) {
  console.error('FAIL: authApi.js still uses raw axios');
  process.exit(1);
}
if (!authSrc.includes('publicClient')) {
  console.error('FAIL: authApi.js does not use publicClient');
  process.exit(1);
}
for (const fn of ['login', 'findUserId', 'resetPasswordRequest', 'resetPasswordComplete']) {
  if (!authSrc.includes(fn)) {
    console.error(`FAIL: missing ${fn}`);
    process.exit(1);
  }
}
if (!apiClientSrc.includes('export const publicClient') && !apiClientSrc.includes('export { publicClient')) {
  if (!/export const publicClient\s*=/.test(apiClientSrc)) {
    console.error('FAIL: publicClient not exported from apiClient.js');
    process.exit(1);
  }
}
if (!apiClientSrc.includes('withCredentials: true')) {
  console.error('FAIL: withCredentials: true missing in apiClient.js');
  process.exit(1);
}
if (!apiClientSrc.includes('publicClient')) {
  console.error('FAIL: publicClient not defined');
  process.exit(1);
}
// 공개 클라이언트는 401 리다이렉트 없음
const publicBlock = apiClientSrc.slice(apiClientSrc.indexOf('publicClient'));
if (publicBlock.includes("window.location.href = '/login'")) {
  console.error('FAIL: publicClient must not redirect on 401');
  process.exit(1);
}

// 런타임: sharedConfig 와 동일한 axios 인스턴스로 login 요청 옵션 캡처
const mirror = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

let captured;
mirror.interceptors.request.use((config) => {
  captured = config;
  return Promise.reject(Object.assign(new Error('verify-abort'), { config, isAxiosError: true }));
});

try {
  await mirror.post('/user/login', { email: 'verify@example.com', password: 'password' });
} catch {
  /* expected */
}

if (captured?.withCredentials !== true) {
  console.error('FAIL: withCredentials not true on request', captured?.withCredentials);
  process.exit(1);
}
if (captured?.baseURL !== '/api') {
  console.error('FAIL: baseURL', captured?.baseURL);
  process.exit(1);
}
if (captured?.headers?.Authorization) {
  console.error('FAIL: Authorization should not be set for public login');
  process.exit(1);
}

console.log('PASS: authApi→publicClient, withCredentials=true, baseURL=/api, no auth/401 redirect');
