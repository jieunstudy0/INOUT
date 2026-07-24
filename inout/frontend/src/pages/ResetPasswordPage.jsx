import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { resetPasswordComplete } from '../api/authApi';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  // URL에서 ?key=... 값을 뽑아옵니다.
  const [searchParams] = useSearchParams();
  const resetKey = searchParams.get('key');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    password: '',
    passwordConfirm: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleReset = async (e) => {
    e.preventDefault();
    if (!resetKey) return setError('유효하지 않은 접근입니다. 이메일 링크를 다시 확인해 주세요.');
    if (formData.password !== formData.passwordConfirm) return setError('비밀번호가 일치하지 않습니다.');
    
    setLoading(true);
    try {
      await resetPasswordComplete(resetKey, formData.password, formData.passwordConfirm);
      alert('비밀번호가 성공적으로 변경되었습니다. 새로운 비밀번호로 로그인해 주세요!');
      navigate('/login');
    } catch (err) {
      console.error("비밀번호 재설정 에러:", err);
      setError(err.response?.data?.header?.message || err.response?.data?.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 실시간 비밀번호 일치 여부 확인 UI용 변수
  const isPasswordMatch = formData.password === formData.passwordConfirm;
  const isPasswordConfirmFilled = formData.passwordConfirm.length > 0;

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-10">
      <div className="p-10 bg-white shadow-xl rounded-2xl w-[480px] border border-slate-200">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-indigo-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-indigo-200">
            <span className="text-white font-bold text-xl">IN</span>
          </div>
          <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">비밀번호 재설정</h1>
          <p className="text-slate-500 text-sm mt-1">새로운 비밀번호를 입력해 주세요.</p>
        </div>

        <form onSubmit={handleReset} className="space-y-5">
          <div>
            <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">New Password *</label>
            <input 
              type="password" name="password" minLength="8" 
              value={formData.password} onChange={handleChange} required 
              placeholder="새로운 비밀번호 (8자 이상)"
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all" 
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Confirm Password *</label>
            <input 
              type="password" name="passwordConfirm" 
              value={formData.passwordConfirm} onChange={handleChange} required 
              placeholder="비밀번호 다시 입력"
              className={`w-full px-4 py-3 bg-slate-50 border rounded-xl focus:outline-none focus:ring-2 focus:border-transparent transition-all ${
                isPasswordConfirmFilled && !isPasswordMatch 
                  ? 'border-rose-300 focus:ring-rose-500' 
                  : 'border-slate-200 focus:ring-indigo-500'
              }`} 
            />
            {isPasswordConfirmFilled && (
              <p className={`text-[11px] font-semibold mt-1.5 ml-1 ${isPasswordMatch ? 'text-emerald-500' : 'text-rose-500'}`}>
                {isPasswordMatch ? '✓ 비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
              </p>
            )}
          </div>

          {error && (
            <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 mt-2">
              {error}
            </p>
          )}

          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 text-white py-3.5 rounded-xl font-bold hover:bg-indigo-700 active:scale-[0.98] transition-all shadow-md shadow-indigo-100 mt-6 disabled:opacity-60 disabled:cursor-not-allowed">
            {loading ? '변경 처리 중...' : '비밀번호 변경하기'}
          </button>
        </form>
      </div>
    </div>
  );
}