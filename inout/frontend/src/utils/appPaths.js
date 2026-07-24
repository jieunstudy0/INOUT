import { useLocation } from 'react-router-dom';
import { getPrimaryRole, homePathByRole } from './roleUtils';

export function getPrimaryRoleFromToken() {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    return getPrimaryRole(payload.roles || payload.auth || '');
  } catch {
    return null;
  }
}

/** 현재 URL 또는 로그인 역할 기준 앱 접두사 (/admin | /owner | /emp) */
export function useAppBasePath() {
  const { pathname } = useLocation();
  if (pathname.startsWith('/owner')) return '/owner';
  if (pathname.startsWith('/admin')) return '/admin';
  if (pathname.startsWith('/emp')) return '/emp';
  const role = getPrimaryRoleFromToken();
  if (role === 'OWNER') return '/owner';
  if (role === 'ADMIN') return '/admin';
  return '/emp';
}

export { homePathByRole, getPrimaryRole };
