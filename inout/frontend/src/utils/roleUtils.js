export function getPrimaryRole(roles) {
  const s = typeof roles === 'string' ? roles : JSON.stringify(roles || '');
  if (s.includes('ADMIN')) return 'ADMIN';
  if (s.includes('OWNER')) return 'OWNER';
  if (s.includes('GUEST')) return 'GUEST';
  return 'EMPLOYEE';
}

export function homePathByRole(role) {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'OWNER') return '/owner/dashboard';
  if (role === 'GUEST') return '/onboarding/complete-profile';
  return '/emp/dashboard';
}

/** 토큰에서 ROLE_GUEST 여부 확인 */
export function isGuestToken(token) {
  try {
    if (!token) return false;
    const payload = JSON.parse(atob(token.split('.')[1]));
    const roles = payload.roles || payload.auth || '';
    return typeof roles === 'string' && roles.includes('GUEST');
  } catch {
    return false;
  }
}

export function resolveHomeFromToken(token, fallbackRole) {
  try {
    if (token && typeof token === 'string' && token.includes('.')) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const roles = payload.roles || payload.auth || '';
      return homePathByRole(getPrimaryRole(roles));
    }
  } catch {
    /* ignore parse errors */
  }
  return homePathByRole(getPrimaryRole(fallbackRole || ''));
}
