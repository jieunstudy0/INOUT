import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { completeSocialOnboarding } from '../api/authApi';
import { homePathByRole } from '../utils/roleUtils';
import { Toast } from '../utils/toast';

const ROLES = [
  {
    value: 'OWNER',
    label: '점주',
    desc: '가맹점을 운영하며 발주 승인·직원 관리를 담당합니다.',
    icon: '🏪',
  },
  {
    value: 'EMPLOYEE',
    label: '직원',
    desc: '재고 확인, 발주 요청, 문의 등 현장 업무를 수행합니다.',
    icon: '👤',
  },
];

function parseTokenPayload(token) {
  try {
    if (!token) return {};
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return {};
  }
}

export default function SocialOnboardingPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [accessToken] = useState(
    () => searchParams.get('accessToken') || localStorage.getItem('accessToken') || '',
  );
  const payload = parseTokenPayload(accessToken);

  const [name, setName] = useState(payload.name || '');
  const [selectedRole, setSelectedRole] = useState('');
  const [phone, setPhone] = useState('');
  const [birthday, setBirthday] = useState('');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // 온보딩 진입 시 임시 토큰 저장 (API 호출에 사용)
  useEffect(() => {
    if (accessToken) localStorage.setItem('accessToken', accessToken);
  }, [accessToken]);

  const validate = () => {
    const errs = {};
    if (!name.trim()) {
      errs.name = '이름을 입력해 주세요.';
    } else if (name.trim().length < 2) {
      errs.name = '이름은 2자 이상 입력해 주세요.';
    } else if (name.trim().length > 50) {
      errs.name = '이름은 50자 이하로 입력해 주세요.';
    }
    if (!selectedRole) errs.role = '역할을 선택해 주세요.';
    if (!phone.trim()) {
      errs.phone = '연락처를 입력해 주세요.';
    } else if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(phone.trim())) {
      errs.phone = '올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)';
    }
    if (!birthday) {
      errs.birthday = '생년월일을 입력해 주세요.';
    } else {
      const d = new Date(birthday);
      const now = new Date();
      if (isNaN(d.getTime())) errs.birthday = '올바른 날짜를 입력해 주세요.';
      else if (d > now) errs.birthday = '생년월일은 오늘 이전이어야 합니다.';
    }
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setSubmitting(true);
    try {
      const result = await completeSocialOnboarding(selectedRole, phone.trim(), birthday, name.trim());
      const newToken = result?.accessToken;
      const newRole = result?.role || '';
      if (newToken) {
        localStorage.setItem('accessToken', newToken);
        if (result?.refreshToken) localStorage.setItem('refreshToken', result.refreshToken);
      }
      Toast.success('회원가입이 완료되었습니다. 환영합니다!');
      navigate(homePathByRole(newRole.replace('ROLE_', '')), { replace: true });
    } catch (err) {
      Toast.error(err?.message || '온보딩 중 오류가 발생했습니다. 다시 시도해 주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-xl border border-slate-200 p-8 sm:p-10">

        {/* 헤더 */}
        <div className="flex flex-col items-center mb-8 text-center">
          <div className="w-12 h-12 bg-indigo-600 rounded-xl flex items-center justify-center mb-4 shadow-lg shadow-indigo-200">
            <span className="text-white text-xl">✦</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900">거의 다 왔어요!</h1>
          <p className="mt-1 text-sm text-slate-500">
            역할과 추가 정보를 입력하면 바로 시작할 수 있어요.
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-6">

          {/* 이메일 (Read-only) */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">이메일</label>
            <input
              readOnly
              value={payload.sub || ''}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-400 cursor-not-allowed"
            />
            <p className="mt-1 text-xs text-slate-400">구글 계정 이메일은 변경할 수 없습니다.</p>
          </div>

          {/* 이름 (편집 가능) */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              이름(실명) <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="실명을 입력해 주세요"
              value={name}
              onChange={(e) => { setName(e.target.value); setErrors((p) => ({ ...p, name: undefined })); }}
              className={[
                'w-full rounded-xl border px-3 py-2.5 text-sm outline-none transition',
                errors.name
                  ? 'border-rose-400 focus:ring-2 focus:ring-rose-200'
                  : 'border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100',
              ].join(' ')}
            />
            {errors.name
              ? <p className="mt-1 text-xs text-rose-500">{errors.name}</p>
              : <p className="mt-1 text-xs text-slate-400">구글 프로필 이름이 자동 입력되었습니다. 실명으로 수정해 주세요.</p>
            }
          </div>

          {/* 역할 선택 */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              역할 선택 <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-2 gap-3">
              {ROLES.map((r) => (
                <button
                  type="button"
                  key={r.value}
                  onClick={() => { setSelectedRole(r.value); setErrors((p) => ({ ...p, role: undefined })); }}
                  className={[
                    'flex flex-col items-center gap-1 rounded-2xl border-2 p-4 text-center transition-all',
                    selectedRole === r.value
                      ? 'border-indigo-500 bg-indigo-50 shadow-md shadow-indigo-100'
                      : 'border-slate-200 bg-white hover:border-slate-300',
                  ].join(' ')}
                >
                  <span className="text-2xl">{r.icon}</span>
                  <span className="text-sm font-bold text-slate-800">{r.label}</span>
                  <span className="text-xs text-slate-500 leading-snug">{r.desc}</span>
                </button>
              ))}
            </div>
            {errors.role && <p className="mt-1.5 text-xs text-rose-500">{errors.role}</p>}
          </div>

          {/* 연락처 */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              연락처 <span className="text-rose-500">*</span>
            </label>
            <input
              type="tel"
              placeholder="010-1234-5678"
              value={phone}
              onChange={(e) => { setPhone(e.target.value); setErrors((p) => ({ ...p, phone: undefined })); }}
              className={[
                'w-full rounded-xl border px-3 py-2.5 text-sm outline-none transition',
                errors.phone
                  ? 'border-rose-400 focus:ring-2 focus:ring-rose-200'
                  : 'border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100',
              ].join(' ')}
            />
            {errors.phone && <p className="mt-1 text-xs text-rose-500">{errors.phone}</p>}
          </div>

          {/* 생년월일 */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              생년월일 <span className="text-rose-500">*</span>
            </label>
            <input
              type="date"
              max={new Date().toISOString().split('T')[0]}
              value={birthday}
              onChange={(e) => { setBirthday(e.target.value); setErrors((p) => ({ ...p, birthday: undefined })); }}
              className={[
                'w-full rounded-xl border px-3 py-2.5 text-sm outline-none transition',
                errors.birthday
                  ? 'border-rose-400 focus:ring-2 focus:ring-rose-200'
                  : 'border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100',
              ].join(' ')}
            />
            {errors.birthday && <p className="mt-1 text-xs text-rose-500">{errors.birthday}</p>}
          </div>

          {/* 제출 */}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-indigo-600 py-3 text-sm font-semibold text-white shadow-md shadow-indigo-200 hover:bg-indigo-700 transition disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {submitting ? '처리 중…' : '가입 완료하기'}
          </button>
        </form>
      </div>
    </div>
  );
}
