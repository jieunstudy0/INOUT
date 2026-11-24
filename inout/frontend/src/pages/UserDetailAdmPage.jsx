import { useState, useEffect, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getUserDetail, updateUserBasicInfo, updateUserByAdmin, unlockUserAccount, sendPasswordResetByAdmin, getStores,
} from '../api/adminUserApi';
import { uploadAdminImage } from '../api/stockApi';
import { toFieldErrors } from '../api/apiClient';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import PhotoField from '../components/common/PhotoField';
import Avatar from '../components/common/Avatar';
import StatusPill from '../components/common/UserStatusPill';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:bg-slate-50 disabled:text-slate-500';
const h3 = 'text-xl font-normal text-slate-900 pb-3 border-b border-slate-900';

/** 본사 — 회원 상세 (/admin/users/:userId): 사진·이름·연락처, 소속 매장·재직 상태·관리자 권한, 잠금 해제·비밀번호 초기화 */
export default function UserDetailAdmPage() {
  const { userId } = useParams();
  const [user, setUser] = useState(null);
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [basic, setBasic] = useState({ name: '', phone: '', profileImageUrl: '' });
  const [account, setAccount] = useState({ storeId: '', status: 'ACTIVE', isAdmin: false });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getUserDetail(userId)
      .then((u) => {
        setUser(u);
        setBasic({ name: u.name || '', phone: u.phone || '', profileImageUrl: u.profileImageUrl || '' });
        setAccount({ storeId: u.storeId != null ? String(u.storeId) : '', status: u.status, isAdmin: !!u.isAdmin });
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, [userId]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { getStores().then((s) => setStores(s || [])).catch(() => {}); }, []);

  const save = async (e) => {
    e.preventDefault();
    if (!basic.profileImageUrl) { Toast.warning('직원 사진을 올려 주세요.'); return; }
    setSaving(true);
    setErrors({});
    try {
      await updateUserBasicInfo(userId, basic);
      const resigned = account.status === 'RESIGNED';
      await updateUserByAdmin(userId, {
        status: account.status,
        // 퇴사하면 소속 매장에서 분리한다
        storeId: resigned || account.storeId === '' ? null : Number(account.storeId),
        isAdmin: account.isAdmin,
      });
      Toast.success('회원 정보를 저장했습니다.');
      load();
    } catch (err) {
      setErrors(toFieldErrors(err?.errors));
    } finally {
      setSaving(false);
    }
  };

  const unlock = async () => {
    if (!window.confirm(`${user.name} 님의 계정 잠금을 해제할까요?`)) return;
    try { await unlockUserAccount(user.id); Toast.success('잠금을 해제했습니다.'); load(); } catch { /* interceptor */ }
  };

  const resetPassword = async () => {
    if (!window.confirm('이 회원의 이메일로 비밀번호 재설정 링크를 보낼까요?')) return;
    try { await sendPasswordResetByAdmin(user.id); Toast.success('메일을 보냈습니다.'); } catch { /* interceptor */ }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner size="lg" /></div>;
  if (!user) return <EmptyState message="회원을 찾을 수 없습니다." />;

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <Link to="/admin/users" className="text-sm text-slate-500 hover:text-slate-900">‹ 회원 관리</Link>
        <div className="mt-3 flex items-center gap-4">
          <Avatar src={user.profileImageUrl} name={user.name} size={56} />
          <div>
            <h2 className="text-3xl font-normal text-slate-900 flex items-center gap-2">{user.name} <StatusPill status={user.status} /></h2>
            <p className="text-sm text-slate-500">{user.email} · {user.storeName} · 가입 {new Date(user.createdAt).toLocaleDateString('ko-KR')}</p>
          </div>
        </div>
      </div>

      <form onSubmit={save} className="space-y-8">
        <section className="grid grid-cols-1 md:grid-cols-[200px_1fr] gap-8">
          <div>
            <p className="text-sm text-slate-700 mb-2 text-center">직원 사진<span className="text-rose-500 ml-0.5">*</span></p>
            <PhotoField shape="circle" value={basic.profileImageUrl} upload={uploadAdminImage}
              onChange={(url) => setBasic((b) => ({ ...b, profileImageUrl: url }))}
              placeholder={<Avatar name={basic.name} size={128} />} />
          </div>
          <div className="space-y-4">
            <h3 className={h3}>기본 정보</h3>
            <label className="block text-sm text-slate-700">이름
              <input value={basic.name} onChange={(e) => setBasic({ ...basic, name: e.target.value })} className={`${inputCls} mt-1.5`} />
              {errors.name && <span className="block text-xs text-rose-600 mt-1">{errors.name}</span>}
            </label>
            <label className="block text-sm text-slate-700">연락처
              <input value={basic.phone} onChange={(e) => setBasic({ ...basic, phone: e.target.value })} className={`${inputCls} mt-1.5`} />
              {errors.phone && <span className="block text-xs text-rose-600 mt-1">{errors.phone}</span>}
            </label>
            <div className="grid grid-cols-2 gap-4 text-sm text-slate-700">
              <label className="block">이메일 (로그인 ID)<input value={user.email} disabled className={`${inputCls} mt-1.5`} /></label>
              <label className="block">생년월일<input value={user.birthday || '-'} disabled className={`${inputCls} mt-1.5`} /></label>
            </div>
          </div>
        </section>

        <section className="space-y-4">
          <h3 className={h3}>소속 · 상태 · 권한</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <label className="block text-sm text-slate-700">소속 매장
              <select value={account.status === 'RESIGNED' ? '' : account.storeId} disabled={account.status === 'RESIGNED'}
                onChange={(e) => setAccount({ ...account, storeId: e.target.value })} className={`${inputCls} mt-1.5`}>
                <option value="">본사 (소속 없음)</option>
                {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </label>
            <label className="block text-sm text-slate-700">재직 상태
              <select value={account.status} onChange={(e) => setAccount({ ...account, status: e.target.value })} className={`${inputCls} mt-1.5`}>
                <option value="ACTIVE">재직</option>
                <option value="ON_LEAVE">휴직</option>
                <option value="RESIGNED">퇴사 (소속 매장 분리)</option>
              </select>
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700 md:mt-7">
              <input type="checkbox" checked={account.isAdmin} onChange={(e) => setAccount({ ...account, isAdmin: e.target.checked })} />
              본사 관리자 권한
            </label>
          </div>
        </section>

        <div className="flex items-center gap-2">
          <button type="submit" disabled={saving} className="px-7 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
            {saving ? '저장 중...' : '저장'}
          </button>
          <Link to="/admin/users" className="px-5 py-2.5 text-sm rounded-full border border-slate-300 text-slate-700 hover:bg-slate-50">목록</Link>
        </div>
      </form>

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
    </div>
  );
}
