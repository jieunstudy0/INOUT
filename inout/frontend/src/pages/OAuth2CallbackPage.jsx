import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { resolveHomeFromToken, getPrimaryRole, homePathByRole } from '../utils/roleUtils';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const role = searchParams.get('role') || '';

    if (!accessToken) {
      setError('소셜 로그인 토큰을 받지 못했습니다. 다시 로그인해 주세요.');
      return;
    }

    localStorage.setItem('accessToken', accessToken);

    const path = resolveHomeFromToken(accessToken, role)
      || homePathByRole(getPrimaryRole(role));

    navigate(path, { replace: true });
  }, [navigate, searchParams]);

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-4">
        <p className="text-sm text-rose-600">{error}</p>
        <button
          type="button"
          onClick={() => navigate('/login', { replace: true })}
          className="px-4 py-2 rounded-xl bg-slate-900 text-white text-sm font-semibold"
        >
          로그인으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <p className="text-sm text-slate-500">소셜 로그인 처리 중...</p>
    </div>
  );
}
