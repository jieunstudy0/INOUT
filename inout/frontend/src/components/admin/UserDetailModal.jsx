import { useState } from 'react';
import { Toast } from '../../utils/toast';
import Spinner from '../common/Spinner';
import { unlockUserAccount, updateUserByAdmin, sendPasswordResetEmail } from '../../api/adminUserApi';

/**
 * 본사 관리자 — 회원 상세 관리 모달
 * 잠긴 계정일 때 [계정 잠금 해제] 노출
 */
export default function UserDetailModal({ user, onClose, onRefresh, onUserPatched }) {
  const [formData, setFormData] = useState({
    status: user.status,
    storeId: user.storeId != null && user.storeId !== '' ? String(user.storeId) : '0',
    isAdmin: user.isAdmin || false,
  });
  const [saving, setSaving] = useState(false);
  const [unlocking, setUnlocking] = useState(false);
  const locked = !!user.isLocked;
  const resigned = formData.status === 'RESIGNED' || user.status === 'RESIGNED';

  const handleUnlock = async () => {
    if (!window.confirm(`${user.name} 님의 계정 잠금을 해제하시겠습니까?\n로그인 실패 횟수가 0으로 초기화됩니다.`)) {
      return;
    }
    setUnlocking(true);
    try {
      await unlockUserAccount(user.id);
      Toast.success('계정 잠금이 해제되었습니다.');
      onUserPatched?.({ ...user, isLocked: false });
      onRefresh?.();
    } catch {
      /* interceptor toast */
    } finally {
      setUnlocking(false);
    }
  };

  const handleResetPassword = async () => {
    if (!window.confirm('해당 사용자의 이메일로 비밀번호 초기화 링크를 발송하시겠습니까?')) return;
    try {
      await sendPasswordResetEmail({ email: user.email, name: user.name, phone: user.phone });
      Toast.success('비밀번호 초기화 메일이 발송되었습니다.');
    } catch {
      /* */
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const payload = {
        ...formData,
        // 퇴사 시 소속 매장 분리 (본점/소속 없음)
        storeId: formData.status === 'RESIGNED' ? null : (formData.storeId === '0' || formData.storeId === '' ? null : Number(formData.storeId)),
      };
      await updateUserByAdmin(user.id, payload);
      Toast.success('회원 정보가 성공적으로 변경되었습니다.');
      onRefresh?.();
      onClose?.();
    } catch {
      /* */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
          <h2 className="text-lg font-bold text-slate-800">회원 상세 관리</h2>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>

        <div className="p-6 space-y-6 overflow-y-auto">
          <div className="flex items-center gap-4 bg-indigo-50/50 p-4 rounded-xl border border-indigo-100">
            <div className="w-12 h-12 bg-indigo-600 text-white rounded-full flex items-center justify-center text-xl font-bold shadow-sm shrink-0">
              {user.name.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className="text-base font-bold text-slate-800">{user.name}</h3>
                {user.isAdmin && (
                  <span className="bg-indigo-600 text-white text-[10px] px-1.5 py-0.5 rounded font-bold">ADMIN</span>
                )}
                {locked ? (
                  <span className="bg-rose-100 text-rose-700 text-[10px] px-1.5 py-0.5 rounded font-bold border border-rose-200">잠김</span>
                ) : (
                  <span className="bg-blue-50 text-blue-700 text-[10px] px-1.5 py-0.5 rounded font-bold border border-blue-100">정상</span>
                )}
              </div>
              <p className="text-sm text-slate-500 truncate">{user.email}</p>
              <p className="text-xs text-slate-400 mt-0.5">{user.phone}</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">소속 매장</label>
              <select
                value={resigned ? '0' : formData.storeId}
                disabled={resigned || formData.status === 'RESIGNED'}
                onChange={(e) => setFormData({ ...formData, storeId: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-slate-100 disabled:text-slate-500"
              >
                <option value="0">본점 (소속 없음)</option>
                <option value="1">강남 1호점</option>
                <option value="2">홍대 2호점</option>
                <option value="3">부산 3호점</option>
              </select>
              {(resigned || formData.status === 'RESIGNED') && (
                <p className="text-[11px] text-slate-400 mt-1">퇴사 처리 시 소속 매장에서 자동 분리됩니다.</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">재직 상태</label>
                <select
                  value={formData.status}
                  onChange={(e) => {
                    const status = e.target.value;
                    setFormData({
                      ...formData,
                      status,
                      storeId: status === 'RESIGNED' ? '0' : formData.storeId,
                    });
                  }}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="ACTIVE">재직 중</option>
                  <option value="ON_LEAVE">휴직</option>
                  <option value="RESIGNED">퇴사</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">권한 승격</label>
                <div className="flex items-center h-9 mt-1">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.isAdmin}
                      onChange={(e) => setFormData({ ...formData, isAdmin: e.target.checked })}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600" />
                    <span className="ml-3 text-sm font-medium text-slate-700">관리자(ADMIN) 권한</span>
                  </label>
                </div>
              </div>
            </div>

            {locked && (
              <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 space-y-2">
                <p className="text-xs font-bold text-rose-700">이 계정은 로그인 실패로 잠겨 있습니다.</p>
                <p className="text-[11px] text-rose-600/80">잠금 해제 시 실패 횟수가 초기화되며, 상단 &apos;잠긴 계정&apos; KPI가 갱신됩니다.</p>
                <button
                  type="button"
                  onClick={handleUnlock}
                  disabled={unlocking}
                  className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-rose-600 text-white text-sm font-bold rounded-xl shadow-sm hover:bg-rose-700 disabled:opacity-50"
                >
                  {unlocking ? <Spinner size="sm" className="text-white" /> : '🔒'} 계정 잠금 해제
                </button>
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={handleResetPassword}
              className="text-sm font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" />
              </svg>
              비밀번호 초기화 메일 발송
            </button>
            <p className="text-xs text-slate-400 mt-1 pl-5">사용자의 이메일로 비밀번호 재설정 링크를 전송합니다.</p>
          </div>
        </div>

        <div className="p-4 border-t border-slate-100 bg-slate-50 flex gap-2 justify-end">
          <button type="button" onClick={onClose} className="px-5 py-2 text-sm font-bold text-slate-600 bg-white border border-slate-300 rounded-xl hover:bg-slate-50">
            취소
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="px-5 py-2 text-sm font-bold text-white bg-slate-800 rounded-xl hover:bg-slate-900 disabled:opacity-50 flex items-center"
          >
            {saving ? <Spinner size="sm" /> : '변경사항 저장'}
          </button>
        </div>
      </div>
    </div>
  );
}
