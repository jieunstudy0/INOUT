import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  getOwnerUserDetail, updateOwnerUserBasicInfo, uploadOwnerImage, createOwnerEmployee,
  updateOwnerEmployeeStatus, updateOwnerEmployeeDepositLimit, unlockOwnerEmployee, resetOwnerEmployeePassword,
} from '../api/ownerUserApi';
import { toFieldErrors } from '../api/apiClient';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import PhotoField from '../components/common/PhotoField';
import Avatar from '../components/common/Avatar';
import StatusPill from '../components/common/UserStatusPill';
import { confirmDialog } from '../utils/dialog';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:bg-slate-50 disabled:text-slate-500';
const h3 = 'text-xl font-normal text-slate-900 pb-3 border-b border-slate-900';

function Err({ msg }) {
  return msg ? <span className="block text-xs text-rose-600 mt-1">{msg}</span> : null;
}

/** 직원 등록 (/owner/users/new): 사진까지 받고, 계정을 만든 뒤 사진을 저장한다. */
function CreateView() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', name: '', password: '', confirmPassword: '', phone: '', birthday: '', profileImageUrl: '' });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const save = async (e) => {
    e.preventDefault();
    if (!form.profileImageUrl) { Toast.warning('직원 사진을 올려 주세요.'); return; }
    if (form.password !== form.confirmPassword) { Toast.warning('비밀번호 확인이 일치하지 않습니다.'); return; }
    setSaving(true);
    setErrors({});
    try {
      const { profileImageUrl, ...account } = form;
      const id = await createOwnerEmployee(account);
      await updateOwnerUserBasicInfo(id, { name: form.name, phone: form.phone, profileImageUrl });
      Toast.success('직원을 등록했습니다.');
      navigate(`/owner/users/${id}`, { replace: true });
    } catch (err) {
      setErrors(toFieldErrors(err?.errors));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <Link to="/owner/users" className="text-sm text-slate-500 hover:text-slate-900">‹ 직원 관리</Link>
        <h2 className="mt-2 text-3xl font-normal text-slate-900">직원 등록</h2>
      </div>
      <form onSubmit={save} className="grid grid-cols-1 md:grid-cols-[200px_1fr] gap-8">
        <div>
          <p className="text-sm text-slate-700 mb-2 text-center">직원 사진<span className="text-rose-500 ml-0.5">*</span></p>
          <PhotoField shape="circle" value={form.profileImageUrl} upload={uploadOwnerImage}
            onChange={(url) => setForm((f) => ({ ...f, profileImageUrl: url }))} placeholder={<Avatar name={form.name} size={128} />} />
        </div>
        <div className="space-y-4">
          <h3 className={h3}>계정 정보</h3>
          <label className="block text-sm text-slate-700">이메일 (로그인 ID)<input type="email" value={form.email} onChange={set('email')} className={`${inputCls} mt-1.5`} /><Err msg={errors.email} /></label>
          <div className="grid grid-cols-2 gap-4">
            <label className="block text-sm text-slate-700">이름<input value={form.name} onChange={set('name')} className={`${inputCls} mt-1.5`} /><Err msg={errors.name} /></label>
            <label className="block text-sm text-slate-700">연락처<input value={form.phone} onChange={set('phone')} placeholder="010-0000-0000" className={`${inputCls} mt-1.5`} /><Err msg={errors.phone} /></label>
            <label className="block text-sm text-slate-700">비밀번호<input type="password" value={form.password} onChange={set('password')} className={`${inputCls} mt-1.5`} /><Err msg={errors.password} /></label>
            <label className="block text-sm text-slate-700">비밀번호 확인<input type="password" value={form.confirmPassword} onChange={set('confirmPassword')} className={`${inputCls} mt-1.5`} /><Err msg={errors.confirmPassword} /></label>
            <label className="block text-sm text-slate-700">생년월일<input type="date" value={form.birthday} onChange={set('birthday')} className={`${inputCls} mt-1.5`} /><Err msg={errors.birthday} /></label>
          </div>
          <div className="flex gap-2 pt-2">
            <button type="submit" disabled={saving} className="px-7 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">{saving ? '등록 중...' : '등록'}</button>
            <Link to="/owner/users" className="px-5 py-2.5 text-sm rounded-full border border-slate-300 text-slate-700 hover:bg-slate-50">취소</Link>
          </div>
        </div>
      </form>
    </div>
  );
}

/** 직원 상세 (/owner/users/:userId): 사진·이름·연락처, 재직 상태, 1일 예치금 한도, 잠금 해제·비밀번호 초기화 */
function DetailView({ userId }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [basic, setBasic] = useState({ name: '', phone: '', profileImageUrl: '' });
  const [status, setStatus] = useState('ACTIVE');
  const [limit, setLimit] = useState({ unlimited: true, value: '' });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getOwnerUserDetail(userId)
      .then((u) => {
        setUser(u);
        setBasic({ name: u.name || '', phone: u.phone || '', profileImageUrl: u.profileImageUrl || '' });
        setStatus(u.status === 'LEAVE' ? 'ON_LEAVE' : u.status);
        setLimit({ unlimited: u.dailyDepositLimit == null, value: u.dailyDepositLimit == null ? '' : String(u.dailyDepositLimit) });
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, [userId]);

  useEffect(() => { load(); }, [load]);

  const isOwnerAccount = user?.roles?.includes('ROLE_OWNER');

  const save = async (e) => {
    e.preventDefault();
    if (!basic.profileImageUrl) { Toast.warning('직원 사진을 올려 주세요.'); return; }
    const nextLimit = limit.unlimited ? null : Number(limit.value);
    if (!limit.unlimited && (limit.value === '' || Number.isNaN(nextLimit) || nextLimit < 0)) {
      Toast.warning('1일 예치금 한도는 0 이상 숫자로 입력해 주세요.');
      return;
    }
    setSaving(true);
    setErrors({});
    try {
      await updateOwnerUserBasicInfo(userId, basic);
      if (!isOwnerAccount) {
        if (status !== user.status) await updateOwnerEmployeeStatus(userId, status);
        if (nextLimit !== (user.dailyDepositLimit ?? null)) await updateOwnerEmployeeDepositLimit(userId, nextLimit);
      }
      Toast.success('직원 정보를 저장했습니다.');
      load();
    } catch (err) {
      setErrors(toFieldErrors(err?.errors));
    } finally {
      setSaving(false);
    }
  };

  const unlock = async () => {
    try { await unlockOwnerEmployee(userId); Toast.success('잠금을 해제했습니다.'); load(); } catch { /* interceptor */ }
  };
  const resetPassword = async () => {
    if (!await confirmDialog(`${user.name} 님에게 비밀번호 재설정 메일을 보낼까요?`)) return;
    try { await resetOwnerEmployeePassword(userId); Toast.success('메일을 보냈습니다.'); } catch { /* interceptor */ }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner size="lg" /></div>;
  if (!user) return <EmptyState message="직원을 찾을 수 없습니다." />;

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <Link to="/owner/users" className="text-sm text-slate-500 hover:text-slate-900">‹ 직원 관리</Link>
        <div className="mt-3 flex items-center gap-4">
          <Avatar src={user.profileImageUrl} name={user.name} size={56} />
          <div>
            <h2 className="text-3xl font-normal text-slate-900 flex items-center gap-2">{user.name} <StatusPill status={user.status} /></h2>
            <p className="text-sm text-slate-500">{user.email} · 가입 {new Date(user.createdAt).toLocaleDateString('ko-KR')}</p>
          </div>
        </div>
      </div>

      <form onSubmit={save} className="space-y-8">
        <section className="grid grid-cols-1 md:grid-cols-[200px_1fr] gap-8">
          <div>
            <p className="text-sm text-slate-700 mb-2 text-center">직원 사진<span className="text-rose-500 ml-0.5">*</span></p>
            <PhotoField shape="circle" value={basic.profileImageUrl} upload={uploadOwnerImage}
              onChange={(url) => setBasic((b) => ({ ...b, profileImageUrl: url }))} placeholder={<Avatar name={basic.name} size={128} />} />
          </div>
          <div className="space-y-4">
            <h3 className={h3}>기본 정보</h3>
            <label className="block text-sm text-slate-700">이름<input value={basic.name} onChange={(e) => setBasic({ ...basic, name: e.target.value })} className={`${inputCls} mt-1.5`} /><Err msg={errors.name} /></label>
            <label className="block text-sm text-slate-700">연락처<input value={basic.phone} onChange={(e) => setBasic({ ...basic, phone: e.target.value })} className={`${inputCls} mt-1.5`} /><Err msg={errors.phone} /></label>
            <div className="grid grid-cols-2 gap-4 text-sm text-slate-700">
              <label className="block">이메일 (로그인 ID)<input value={user.email} disabled className={`${inputCls} mt-1.5`} /></label>
              <label className="block">생년월일<input value={user.birthday || '-'} disabled className={`${inputCls} mt-1.5`} /></label>
            </div>
          </div>
        </section>

        {!isOwnerAccount && (
          <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="space-y-3">
              <h3 className={h3}>재직 상태</h3>
              {[['ACTIVE', '재직', '정상 근무 · 로그인 가능'], ['ON_LEAVE', '휴직', '일시 휴직 · 로그인 차단'], ['RESIGNED', '퇴사', '로그인 차단']].map(([v, label, desc]) => (
                <label key={v} className={`flex items-start gap-3 p-3.5 rounded-[10px] border cursor-pointer ${status === v ? 'border-indigo-600 bg-indigo-50' : 'border-slate-200'}`}>
                  <input type="radio" name="status" checked={status === v} onChange={() => setStatus(v)} className="mt-1" />
                  <span><span className="block text-sm font-semibold text-slate-900">{label}</span><span className="block text-xs text-slate-500">{desc}</span></span>
                </label>
              ))}
            </div>
            <div className="space-y-3">
              <h3 className={h3}>1일 예치금 사용 한도</h3>
              <p className="text-sm text-slate-600">오늘 사용 {(user.todayUsedDeposit ?? 0).toLocaleString('ko-KR')}원</p>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={limit.unlimited} onChange={(e) => setLimit({ unlimited: e.target.checked, value: e.target.checked ? '' : limit.value })} />
                한도 없음
              </label>
              <input type="number" min="0" step="1000" disabled={limit.unlimited} value={limit.value} placeholder="예: 500000"
                onChange={(e) => setLimit({ ...limit, value: e.target.value })} className={inputCls} />
              <p className="text-xs text-slate-500">원 단위. 결제 시 한도를 넘으면 결제가 거부됩니다.</p>
            </div>
          </section>
        )}

        <div className="flex items-center gap-2">
          <button type="submit" disabled={saving} className="px-7 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">{saving ? '저장 중...' : '저장'}</button>
          <Link to="/owner/users" className="px-5 py-2.5 text-sm rounded-full border border-slate-300 text-slate-700 hover:bg-slate-50">목록</Link>
        </div>
      </form>

      {!isOwnerAccount && (
        <section className="space-y-3">
          <h3 className={h3}>계정</h3>
          {user.isLocked && (
            <div className="flex items-center justify-between gap-3 rounded-[10px] bg-rose-50 px-5 py-4">
              <p className="text-sm text-rose-700">로그인 실패가 반복되어 잠긴 계정입니다.</p>
              <button type="button" onClick={unlock} className="px-4 py-2 text-sm rounded-full border border-rose-500 text-rose-600 hover:bg-white">잠금 해제</button>
            </div>
          )}
          <div className="flex items-center justify-between gap-3 rounded-[10px] bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-600">이메일로 비밀번호 재설정 링크를 보냅니다.</p>
            <button type="button" onClick={resetPassword} className="px-4 py-2 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-white">재설정 메일 보내기</button>
          </div>
        </section>
      )}
    </div>
  );
}

export default function UserDetailOwnerPage() {
  const { userId } = useParams();
  return userId === 'new' ? <CreateView /> : <DetailView userId={userId} />;
}
