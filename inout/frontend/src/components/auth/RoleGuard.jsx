import { Navigate, useLocation } from 'react-router-dom';
import { getPrimaryRole, homePathByRole, isGuestToken } from '../../utils/roleUtils';

function parseRoleFromToken() {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    return getPrimaryRole(payload.roles || payload.auth || '');
  } catch {
    return null;
  }
}

function expectedRoleForPath(pathname) {
  if (pathname.startsWith('/admin')) return 'ADMIN';
  if (pathname.startsWith('/owner')) return 'OWNER';
  if (pathname.startsWith('/emp')) return 'EMPLOYEE';
  return null;
}

/**
 * 역할별 URL 접두사 가드.
 * - 미로그인 → /login
 * - 권한 불일치 → 본인 역할 홈으로 리다이렉트 (state.forbidden으로 안내 가능)
 */
export default function RoleGuard({ children, allow }) {
  const location = useLocation();
  const role = parseRoleFromToken();

  if (!role) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  // GUEST 토큰 → 온보딩 완료 전이므로 업무 페이지 접근 차단
  if (role === 'GUEST') {
    return <Navigate to="/onboarding/complete-profile" replace />;
  }

  const required = allow || expectedRoleForPath(location.pathname);
  if (required && role !== required) {
    return (
      <Navigate
        to={homePathByRole(role)}
        replace
        state={{ forbidden: true, attempted: location.pathname }}
      />
    );
  }

  return children;
}

export function ForbiddenNotice() {
  const location = useLocation();
  if (!location.state?.forbidden) return null;
  return (
    <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
      접근 권한이 없는 페이지입니다. 본인 권한의 메인 화면으로 이동했습니다.
    </div>
  );
}
