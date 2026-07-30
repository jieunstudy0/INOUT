import client, { publicClient, unwrap } from './apiClient';

export function login(email, password) {
  return unwrap(publicClient.post('/user/login', { email, password }));
}

export function logout() {
  return unwrap(client.post('/user/logout'));
}

/** 아이디(이메일) 찾기 — 이름 + 연락처 */
export function findUserId(name, phone) {
  return unwrap(publicClient.post('/user/find', { name, phone }));
}

/** 비밀번호 재설정 메일 요청 */
export function resetPasswordRequest(email, name, phone) {
  return unwrap(publicClient.post('/user/public/password/reset', { email, name, phone }));
}

/** 메일 링크의 resetKey로 새 비밀번호 확정 */
export function resetPasswordComplete(resetKey, newPassword, confirmPassword) {
  return unwrap(
    publicClient.post('/user/resetPassword', null, {
      params: { resetKey, newPassword, confirmPassword },
    }),
  );
}
