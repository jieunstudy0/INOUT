import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { checkEmailDuplicate, registerUser, getStoreList } from '../api/authApi';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [emailChecked, setEmailChecked] = useState(false);
  
  const [stores, setStores] = useState([]);

  const [formData, setFormData] = useState({
    email: '', name: '', password: '', passwordConfirm: '', phone: '', birthday: '', storeId: ''
  });

  // 💡 [추가된 부분] 오늘 날짜 기준으로 15년 전의 날짜를 계산 (YYYY-MM-DD 형식)
  const getMaxAllowedDate = () => {
    const today = new Date();
    const year = today.getFullYear() - 15; // 15년 전
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  const maxAllowedDate = getMaxAllowedDate();

  useEffect(() => {
    const fetchStores = async () => {
      try {
        const storeData = await getStoreList();
        setStores(storeData || []);
      } catch (err) {
        console.error('매장 목록을 불러오지 못했습니다.', err);
        setError('매장 정보를 불러오는 중 오류가 발생했습니다.');
      }
    };
    fetchStores();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (name === 'email') setEmailChecked(false);
    setError('');
  };

  const handleCheckEmail = async () => {
    if (!formData.email.trim()) {
      setError('이메일을 먼저 입력해 주세요.');
      return;
    }
    setLoading(true);
    try {
      await checkEmailDuplicate(formData.email);
      setEmailChecked(true);
      setError('');
      alert('사용 가능한 이메일입니다.');
    } catch (err) {
      setEmailChecked(false);
      setError(err.response?.data?.header?.message || err.response?.data?.message || '이미 사용 중인 이메일입니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!emailChecked) return setError('이메일 중복 확인을 진행해 주세요.');
    if (formData.password !== formData.passwordConfirm) return setError('비밀번호가 일치하지 않습니다.');
    if (!formData.name || !formData.phone || !formData.storeId) return setError('필수 항목을 모두 입력해 주세요.');
    
    if (formData.phone.includes('-')) {
      return setError('연락처는 하이픈(-) 없이 숫자만 입력해 주세요.');
    }
    setLoading(true);
    try {
      const payload = {
        email: formData.email,
        name: formData.name,
        password: formData.password,
        confirmPassword: formData.passwordConfirm,
        phone: formData.phone,
        birthday: formData.birthday || null,
        storeId: Number(formData.storeId)
      };

      await registerUser(payload);
      
      alert('회원가입이 완료되었습니다. 로그인해 주세요.');
      navigate('/login');
    } catch (err) {
      console.error("회원가입 에러:", err.response);
      setError(err.response?.data?.header?.message || err.response?.data?.message || '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const isPasswordMatch = formData.password === formData.passwordConfirm;
  const isPasswordConfirmFilled = formData.passwordConfirm.length > 0;

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-10">
      <div className="p-10 bg-white shadow-xl rounded-2xl w-[480px] border border-slate-200">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-indigo-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-indigo-200">
            <span className="text-white font-bold text-xl">IN</span>
          </div>
          <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">회원가입</h1>
          <p className="text-slate-500 text-sm mt-1">INOUT 시스템 시작하기</p>
        </div>

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Email *</label>
            <div className="flex gap-2">
              <input
                type="email" name="email" value={formData.email} onChange={handleChange} required
                placeholder="이메일 주소"
                className="flex-1 px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
              />
              <button
                type="button" onClick={handleCheckEmail} disabled={emailChecked || loading}
                className={`px-4 py-3 rounded-xl font-bold text-sm transition-all whitespace-nowrap ${
                  emailChecked 
                    ? 'bg-emerald-100 text-emerald-700 cursor-not-allowed' 
                    : 'bg-slate-800 text-white hover:bg-slate-900 active:scale-[0.98]'
                }`}
              >
                {emailChecked ? '확인완료' : '중복확인'}
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Password *</label>
              <input type="password" name="password" minLength="8" value={formData.password} onChange={handleChange} required placeholder="8자 이상"
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Confirm *</label>
              <input type="password" name="passwordConfirm" value={formData.passwordConfirm} onChange={handleChange} required placeholder="비밀번호 확인"
                className={`w-full px-4 py-3 bg-slate-50 border rounded-xl focus:outline-none focus:ring-2 focus:border-transparent transition-all ${
                  isPasswordConfirmFilled && !isPasswordMatch 
                    ? 'border-rose-300 focus:ring-rose-500' 
                    : 'border-slate-200 focus:ring-indigo-500'
                }`} />
              
              {isPasswordConfirmFilled && (
                <p className={`text-[11px] font-semibold mt-1.5 ml-1 ${isPasswordMatch ? 'text-emerald-500' : 'text-rose-500'}`}>
                  {isPasswordMatch ? '✓ 비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
                </p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Name *</label>
              <input type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="이름"
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Phone *</label>
              <input 
                type="tel" 
                name="phone" 
                value={formData.phone} 
                onChange={handleChange} 
                required 
                placeholder="01000000000" 
                maxLength={11} 
                className={`w-full px-4 py-3 bg-slate-50 border rounded-xl focus:outline-none focus:ring-2 transition-all ${
                  formData.phone.includes('-') 
                    ? 'border-rose-300 focus:ring-rose-500 focus:border-transparent' 
                    : 'border-slate-200 focus:ring-indigo-500 focus:border-transparent'
                }`} 
              />

              {formData.phone.includes('-') && (
                <p className="text-[11px] font-semibold text-rose-500 mt-1.5 ml-1">
                  - 없이 숫자만 입력해 주세요.
                </p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Store *</label>
              <div className="relative">
                <select 
                  name="storeId" 
                  value={formData.storeId} 
                  onChange={handleChange} 
                  required 
                  className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all text-sm appearance-none cursor-pointer text-slate-700 font-medium"
                >
                  <option value="" disabled>매장을 선택하세요</option>
                  {stores.map((store) => (
                    <option key={store.id} value={store.id}>
                      {store.name}
                    </option>
                  ))}
                </select>
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-slate-400">
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 24">
                    <path d="M7 10l5 5 5-5H7z"/>
                  </svg>
                </div>
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase mb-1 ml-1">Birthday</label>
              <input 
                type="date" 
                name="birthday" 
                value={formData.birthday} 
                onChange={handleChange}
                max={maxAllowedDate} // 💡 [수정된 부분] 15년 전 날짜까지만 선택 가능하도록 제한
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all text-slate-600" 
              />
            </div>
          </div>

          {error && (
            <p className="text-xs text-rose-600 bg-rose-50 border border-rose-100 rounded-lg px-3 py-2 mt-2">
              {error}
            </p>
          )}

          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 text-white py-3.5 rounded-xl font-bold hover:bg-indigo-700 active:scale-[0.98] transition-all shadow-md shadow-indigo-100 mt-4 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2">
            {loading ? '가입 처리 중...' : '가입하기'}
          </button>
        </form>

        <div className="mt-6 text-center text-xs text-slate-400">
          이미 계정이 있으신가요? <Link to="/login" className="text-indigo-500 hover:text-indigo-600 ml-1 font-semibold transition-colors">로그인으로 돌아가기</Link>
        </div>
      </div>
    </div>
  );
}