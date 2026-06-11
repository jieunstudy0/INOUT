import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';

export default function LoginPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

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
        
        let isAdmin = false;
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const roles = payload.roles || payload.auth || '';
          isAdmin = (typeof roles === 'string' ? roles : JSON.stringify(roles)).includes('ADMIN');
        } catch (e) {
          console.error("토큰 파싱 에러:", e);
        }

        setTimeout(() => {
          if (isAdmin) {
            navigate('/admin/dashboard', { replace: true }); 
          } else {
            navigate('/emp/dashboard', { replace: true });   
          }
        }, 100);
        
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

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center">
      <div className="p-10 bg-white shadow-xl rounded-2xl w-[400px] border border-slate-200">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-indigo-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-indigo-200">
            <span className="text-white font-bold text-xl">IN</span>
          </div>
          <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">INOUT SYSTEM</h1>
          <p className="text-slate-500 text-sm mt-1">재고 및 주문 관리 시스템</p>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">ID</label>
            <input
              type="text"
              name="email"
              value={formData.email}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              placeholder="아이디를 입력하세요"
              autoComplete="username"
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              placeholder="••••••••"
              autoComplete="current-password"
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
            />
          </div>

          {error && (
            <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <button
            onClick={handleLogin}
            disabled={loading}
            className="w-full bg-indigo-600 text-white py-3 rounded-xl font-bold hover:bg-indigo-700 active:scale-[0.98] transition-all shadow-md shadow-indigo-100 mt-2 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
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

        <div className="mt-8 pt-6 border-t border-slate-100 flex justify-between text-xs text-slate-400">
          <span className="hover:text-indigo-500 cursor-pointer transition-colors">아이디/비밀번호 찾기</span>
          <span className="hover:text-indigo-500 cursor-pointer transition-colors">회원가입</span>
        </div>
      </div>
    </div>
  );
}
