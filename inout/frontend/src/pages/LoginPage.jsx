import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../api/authApi';
import { Toast } from '../utils/toast';
import { resolveHomeFromToken } from '../utils/roleUtils';

// enabled: false 인 provider는 버튼에 "준비중" 배지가 붙고 클릭해도 이동하지 않는다.
// 콘솔 등록/검수/사업자등록 등이 끝나 실제로 로그인이 가능해지면 enabled만 true로 바꾸면 된다.
// (Kakao: 사업자등록번호로 비즈 앱 전환 후 이메일 동의항목 활성화 시 / Naver: 검수 통과 시)
const OAUTH_PROVIDERS = [
  {
    id: 'kakao',
    label: '카카오로 시작하기',
    enabled: false,
    className: 'bg-[#FEE500] text-[#000000] hover:bg-[#FADA0A]',
    disabledClassName: 'bg-[#FEE500]/50 text-[#3c3527]/60 cursor-not-allowed',
  },
  {
    id: 'google',
    label: 'Google로 시작하기',
    enabled: true,
    className: 'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50',
    disabledClassName: 'bg-slate-100 text-slate-400 border border-slate-200 cursor-not-allowed',
  },
  {
    id: 'naver',
    label: '네이버로 시작하기',
    enabled: false,
    className: 'bg-[#03C75A] text-white hover:bg-[#02b351]',
    disabledClassName: 'bg-[#03C75A]/40 text-white/70 cursor-not-allowed',
  },
];

export default function LoginPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleLogin = async () => {
    if (!formData.email.trim() || !formData.password.trim()) {
      setError('아이디와 비밀번호를 모두 입력해 주세요.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const body = await login(formData.email, formData.password);
      const token = body?.accessToken || body?.token || body;

      if (token && typeof token === 'string') {
        localStorage.setItem('accessToken', token);
        if (body?.refreshToken) localStorage.setItem('refreshToken', body.refreshToken);

        const home = resolveHomeFromToken(token, body?.role);
        setTimeout(() => navigate(home, { replace: true }), 100);
      } else {
        setError('로그인 응답 형식이 올바르지 않습니다.');
      }
    } catch (err) {
      setError(err?.message || '아이디 또는 비밀번호를 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleLogin();
  };

  const handleSocialLogin = (provider) => {
    if (!provider.enabled) {
      const name = provider.label.replace('로 시작하기', '');
      Toast.info(`${name} 로그인은 현재 준비 중입니다. 곧 지원할 예정이에요.`);
      return;
    }
    // Spring Security OAuth2 진입점 — 백엔드가 인가 후 /oauth2/callback 으로 리다이렉트
    window.location.href = `/oauth2/authorization/${provider.id}`;
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-10">
      <div className="p-8 sm:p-10 bg-white shadow-xl rounded-3xl w-full max-w-[420px] border border-slate-200">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-indigo-600 rounded-xl flex items-center justify-center mb-4 shadow-lg shadow-indigo-200">
            <span className="text-white font-bold text-xl tracking-tight">IN</span>
          </div>
          <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">INOUT SYSTEM</h1>
          <p className="text-slate-500 text-sm mt-1">재고 및 주문 관리 시스템</p>
        </div>

        <div className="space-y-4">
          <div>
            <input
              type="text"
              name="email"
              value={formData.email}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              placeholder="통합계정 또는 이메일"
              autoComplete="username"
              className="w-full px-4 py-3.5 bg-white border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all placeholder:text-slate-400"
            />
          </div>
          <div>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              placeholder="비밀번호"
              autoComplete="current-password"
              className="w-full px-4 py-3.5 bg-white border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all placeholder:text-slate-400"
            />
          </div>

          <div className="flex items-center pl-1 pt-1 pb-1">
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" className="w-4 h-4 rounded text-indigo-600 border-slate-300 focus:ring-indigo-500" />
              <span className="text-sm text-slate-600">자동 로그인</span>
            </label>
          </div>

          {error && (
            <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 text-center">
              {error}
            </p>
          )}

          <button
            onClick={handleLogin}
            disabled={loading}
            className="w-full bg-[#111111] text-white py-3.5 rounded-xl font-bold hover:bg-black active:scale-[0.98] transition-all mt-2 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                로그인 중...
              </>
            ) : '로그인'}
          </button>
        </div>

        <div className="relative my-7 flex items-center justify-center">
          <div className="w-full h-px bg-slate-200"></div>
          <span className="absolute bg-white px-3 text-xs font-medium text-slate-400">간편 로그인</span>
        </div>

        <div className="space-y-2.5">
          {OAUTH_PROVIDERS.map((provider) => (
            <button
              key={provider.id}
              type="button"
              onClick={() => handleSocialLogin(provider)}
              aria-disabled={!provider.enabled}
              className={`w-full flex items-center justify-center gap-2 py-3.5 rounded-xl font-bold active:scale-[0.98] transition-all ${
                provider.enabled ? provider.className : provider.disabledClassName
              }`}
            >
              <span>{provider.label}</span>
              {!provider.enabled && (
                <span className="text-[10px] font-semibold bg-black/10 px-2 py-0.5 rounded-full">
                  준비중
                </span>
              )}
            </button>
          ))}
        </div>

        <div className="mt-7 mb-5 text-center">
          <Link
            to="/find-account"
            className="text-[13px] font-medium text-slate-400 hover:text-slate-600 underline underline-offset-4 transition-colors"
          >
            아이디 / 비밀번호 찾기
          </Link>
        </div>

        <div>
          <button
            type="button"
            onClick={() => Toast.info('공개 회원가입은 종료되었습니다. 매장 점주에게 계정 생성을 요청해 주세요.')}
            className="w-full flex items-center justify-center bg-white text-slate-500 py-3.5 rounded-xl font-bold border border-slate-200 hover:bg-slate-50 active:scale-[0.98] transition-all"
          >
            이메일로 가입하기 (비활성)
          </button>
        </div>
      </div>
    </div>
  );
}
