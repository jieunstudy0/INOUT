import { useState } from 'react';
import { Link } from 'react-router-dom';
import { findUserId, resetPasswordRequest } from '../api/authApi';

export default function FindAccountPage() {
  const [activeTab, setActiveTab] = useState('ID'); // 'ID' | 'PW'
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  
  // 폼 상태
  const [formData, setFormData] = useState({ email: '', name: '', phone: '' });
  
  // 결과 상태
  const [foundEmail, setFoundEmail]   = useState(null);
  const [pwResetSent, setPwResetSent] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleTabSwitch = (tab) => {
    setActiveTab(tab);
    setFoundEmail(null);
    setPwResetSent(false);
    setError('');
    setFormData({ email: '', name: '', phone: '' });
  };

  // 아이디 찾기 로직
  const handleFindId = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.phone) return setError('이름과 연락처를 입력해 주세요.');
    
    setLoading(true);
    try {
      const user = await findUserId(formData.name, formData.phone);
      setFoundEmail(user.email);
    } catch (err) {
      setError(err.response?.data?.message || '일치하는 계정 정보가 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 비밀번호 찾기 (메일 전송) 로직
  const handleFindPw = async (e) => {
    e.preventDefault();
    if (!formData.email || !formData.name || !formData.phone) return setError('모든 정보를 입력해 주세요.');
    
    setLoading(true);
    try {
      await resetPasswordRequest(formData.email, formData.name, formData.phone);
      setPwResetSent(true);
    } catch (err) {
      setError(err.response?.data?.message || '입력하신 정보와 일치하는 계정이 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  const InputClass = "w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all";
  const ButtonClass = "w-full bg-indigo-600 text-white py-3.5 rounded-xl font-bold hover:bg-indigo-700 active:scale-[0.98] transition-all shadow-md shadow-indigo-100 mt-2 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center";

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-10">
      <div className="bg-white shadow-xl rounded-2xl w-[400px] border border-slate-200 overflow-hidden">
        
        {/* 탭 버튼 영역 */}
        <div className="flex bg-slate-50 border-b border-slate-200">
          <button onClick={() => handleTabSwitch('ID')} className={`flex-1 py-4 text-sm font-bold transition-colors ${activeTab === 'ID' ? 'text-indigo-600 bg-white border-b-2 border-indigo-600' : 'text-slate-400 hover:text-slate-600'}`}>
            아이디 찾기
          </button>
          <button onClick={() => handleTabSwitch('PW')} className={`flex-1 py-4 text-sm font-bold transition-colors ${activeTab === 'PW' ? 'text-indigo-600 bg-white border-b-2 border-indigo-600' : 'text-slate-400 hover:text-slate-600'}`}>
            비밀번호 찾기
          </button>
        </div>

        <div className="p-10">
          {/* ─── 아이디 찾기 화면 ─── */}
          {activeTab === 'ID' && (
            !foundEmail ? (
              <form onSubmit={handleFindId} className="space-y-4">
                <p className="text-xs text-slate-500 mb-6 leading-relaxed">
                  가입 시 등록한 이름과 연락처를 입력해 주세요.
                </p>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Name</label>
                  <input type="text" name="name" value={formData.name} onChange={handleChange} className={InputClass} placeholder="이름" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Phone</label>
                  <input type="tel" name="phone" value={formData.phone} onChange={handleChange} className={InputClass} placeholder="010-0000-0000" />
                </div>
                {error && <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 mt-2">{error}</p>}
                <button type="submit" disabled={loading} className={ButtonClass}>
                  {loading ? '찾는 중...' : '아이디 찾기'}
                </button>
              </form>
            ) : (
              <div className="text-center py-4">
                <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-5 text-2xl shadow-inner">✓</div>
                <h3 className="text-lg font-bold text-slate-800 mb-2">계정을 찾았습니다</h3>
                <div className="bg-slate-50 p-4 rounded-xl font-mono text-indigo-600 font-bold mb-8 border border-slate-200 text-lg shadow-sm">
                  {foundEmail}
                </div>
                <Link to="/login" className={ButtonClass}>로그인 하러가기</Link>
              </div>
            )
          )}

          {/* ─── 비밀번호 찾기 화면 ─── */}
          {activeTab === 'PW' && (
            !pwResetSent ? (
              <form onSubmit={handleFindPw} className="space-y-4">
                <p className="text-xs text-slate-500 mb-6 leading-relaxed">
                  정보를 입력하시면 가입된 이메일로<br/>비밀번호 초기화 링크를 발송해 드립니다.
                </p>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Email</label>
                  <input type="email" name="email" value={formData.email} onChange={handleChange} className={InputClass} placeholder="가입한 이메일" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Name</label>
                  <input type="text" name="name" value={formData.name} onChange={handleChange} className={InputClass} placeholder="이름" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Phone</label>
                  <input type="tel" name="phone" value={formData.phone} onChange={handleChange} className={InputClass} placeholder="010-0000-0000" />
                </div>
                {error && <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 mt-2">{error}</p>}
                <button type="submit" disabled={loading} className={ButtonClass}>
                  {loading ? '메일 전송 중...' : '초기화 메일 전송'}
                </button>
              </form>
            ) : (
              <div className="text-center py-4">
                <div className="w-16 h-16 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-5 text-2xl shadow-inner">✉️</div>
                <h3 className="text-lg font-bold text-slate-800 mb-2">메일 전송 완료</h3>
                <p className="text-xs text-slate-500 mb-8 leading-relaxed bg-slate-50 p-4 rounded-xl border border-slate-200">
                  <strong className="text-indigo-600 font-semibold">{formData.email}</strong>(으)로<br/>비밀번호 재설정 링크를 발송했습니다.<br/>(유효시간 30분)
                </p>
                <Link to="/login" className="w-full bg-slate-800 text-white py-3.5 rounded-xl font-bold hover:bg-slate-900 active:scale-[0.98] transition-all shadow-md flex items-center justify-center">
                  로그인 화면으로
                </Link>
              </div>
            )
          )}
        </div>
        
        {/* 하단 로그인 돌아가기 링크 */}
        {(!foundEmail && !pwResetSent) && (
           <div className="pb-8 text-center text-xs text-slate-400">
             <Link to="/login" className="hover:text-indigo-500 font-semibold transition-colors">← 로그인으로 돌아가기</Link>
           </div>
        )}
      </div>
    </div>
  );
}