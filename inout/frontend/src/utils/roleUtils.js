export function getPrimaryRole(roles) {
  const s = typeof roles === 'string' ? roles : JSON.stringify(roles || '');
  if (s.includes('ADMIN')) return 'ADMIN';
  if (s.includes('OWNER')) return 'OWNER';
  return 'EMPLOYEE';
}

export function homePathByRole(role) {
  if (role === 'ADMIN') return '/admin/dashboard';
  if (role === 'OWNER') return '/owner/dashboard';
  return '/emp/dashboard';
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
