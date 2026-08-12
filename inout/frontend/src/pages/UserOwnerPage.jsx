import { useState, useEffect, useCallback } from 'react';
import {
  getOwnerUsers,
  createOwnerEmployee,
  updateOwnerEmployeeStatus,
  updateOwnerEmployeeDepositLimit,
  unlockOwnerEmployee,
  resetOwnerEmployeePassword,
} from '../api/ownerUserApi.js';
import { Toast } from '../utils/toast.js';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: '재직', desc: '정상 근무 · 로그인 가능', cls: 'border-emerald-200 bg-emerald-50 text-emerald-800' },
  { value: 'ON_LEAVE', label: '휴직', desc: '일시 휴직 · 로그인 차단', cls: 'border-amber-200 bg-amber-50 text-amber-800' },
  { value: 'RESIGNED', label: '퇴사', desc: '퇴사 처리 · 로그인 차단 · 소속 매장 분리', cls: 'border-slate-200 bg-slate-50 text-slate-600' },
];

function StatusBadge({ status }) {
  const config = {
    ACTIVE: { label: '재직', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    ON_LEAVE: { label: '휴직', cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    LEAVE: { label: '휴직', cls: 'bg-amber-100 text-amber-700 border-amber-200' }, // 구버전 호환
    RESIGNED: { label: '퇴사', cls: 'bg-slate-100 text-slate-500 border-slate-200' },
  }[status] || { label: status, cls: 'bg-slate-100 text-slate-600' };
  return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${config.cls}`}>{config.label}</span>;
}

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const btnBase = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';
  return (
    <div className="flex items-center gap-1 mt-6">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${btnBase} ${page === 0 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${btnBase} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${btnBase} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

function EmployeeStatusModal({ user, onClose, onSaved }) {
  const initial = user.status === 'LEAVE' ? 'ON_LEAVE' : (user.status || 'ACTIVE');
  const [selected, setSelected] = useState(initial);
  const [limitInput, setLimitInput] = useState(
    user.dailyDepositLimit == null ? '' : String(user.dailyDepositLimit)
  );
  const [unlimited, setUnlimited] = useState(user.dailyDepositLimit == null);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    const statusChanged = selected !== initial;
    const nextLimit = unlimited ? null : Number(limitInput);
    if (!unlimited && (Number.isNaN(nextLimit) || nextLimit < 0)) {
      Toast.error('1일 예치금 한도는 0 이상 숫자로 입력해 주세요.');
      return;
    }
    const prevLimit = user.dailyDepositLimit ?? null;
    const limitChanged = (unlimited ? null : nextLimit) !== prevLimit
      && !(unlimited && prevLimit == null);

    if (!statusChanged && !limitChanged) {
      onClose();
      return;
    }

    setSaving(true);
    try {
      if (statusChanged) {
        await updateOwnerEmployeeStatus(user.id, selected);
      }
      if (limitChanged) {
        await updateOwnerEmployeeDepositLimit(user.id, unlimited ? null : nextLimit);
      }
      Toast.success('직원 정보가 저장되었습니다.');
      onSaved({
        status: selected,
        dailyDepositLimit: unlimited ? null : nextLimit,
        todayUsedDeposit: user.todayUsedDeposit ?? 0,
      });
      onClose();
    } catch {
      /* interceptor */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-100">
          <h3 className="text-lg font-bold text-slate-800">직원 관리</h3>
          <p className="text-sm text-slate-500 mt-1">
            <span className="font-semibold text-slate-700">{user.name}</span>
            <span className="mx-1.5 text-slate-300">·</span>
            <span className="font-mono text-xs">{user.email}</span>
          </p>
          <p className="text-xs text-slate-400 mt-2 flex items-center gap-1.5">
            현재 상태: <StatusBadge status={user.status} />
          </p>
        </div>

        <div className="px-6 py-5 space-y-4">
          <div className="space-y-2.5">
            <p className="text-xs font-bold text-slate-600 uppercase tracking-wide">계정 상태</p>
            {STATUS_OPTIONS.map((opt) => {
              const active = selected === opt.value;
              return (
                <label
                  key={opt.value}
                  className={`flex items-start gap-3 p-3.5 rounded-xl border-2 cursor-pointer transition-all ${
                    active ? opt.cls + ' border-current shadow-sm' : 'border-slate-100 bg-white hover:border-slate-200'
                  }`}
                >
                  <input
                    type="radio"
                    name="employee-status"
                    value={opt.value}
                    checked={active}
                    onChange={() => setSelected(opt.value)}
                    className="mt-1 accent-indigo-600"
                  />
                  <span className="flex-1">
                    <span className="block text-sm font-bold text-slate-800">{opt.label}</span>
                    <span className="block text-xs text-slate-500 mt-0.5">{opt.desc}</span>
                  </span>
                </label>
              );
            })}
          </div>

          <div className="pt-3 border-t border-slate-100 space-y-2">
            <p className="text-xs font-bold text-slate-600 uppercase tracking-wide">1일 예치금 사용 한도</p>
            <p className="text-[11px] text-slate-400">
              오늘 사용액: {(user.todayUsedDeposit ?? 0).toLocaleString('ko-KR')}원
              {user.dailyDepositLimit != null && (
                <> · 잔여 {(Math.max(0, user.dailyDepositLimit - (user.todayUsedDeposit ?? 0))).toLocaleString('ko-KR')}원</>
              )}
            </p>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={unlimited}
                onChange={(e) => {
                  setUnlimited(e.target.checked);
                  if (e.target.checked) setLimitInput('');
                }}
                className="accent-indigo-600"
              />
              무제한
            </label>
            <input
              type="number"
              min="0"
              step="1000"
              disabled={unlimited}
              value={limitInput}
              onChange={(e) => setLimitInput(e.target.value)}
              placeholder="예: 500000"
              className="w-full px-3 py-2.5 border border-slate-200 rounded-xl text-sm disabled:bg-slate-100 disabled:text-slate-400"
            />
            <p className="text-[11px] text-slate-400">원 단위. 결제 시 한도를 초과하면 결제가 거부됩니다.</p>
          </div>
        </div>

        <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex gap-2">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 rounded-xl bg-white border border-slate-200 text-slate-600 text-sm font-medium hover:bg-slate-100">
            취소
          </button>
          <button
            type="button"
            disabled={saving}
            onClick={handleSave}
            className="flex-1 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-700 disabled:opacity-50"
          >
            {saving ? '저장 중…' : '변경 저장'}
          </button>
        </div>
      </div>
    </div>
  );
}

const emptyForm = {
  email: '', name: '', password: '', confirmPassword: '', phone: '', birthday: '',
};

export default function UserOwnerPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState({ total: 0, active: 0, leave: 0, locked: 0, resigned: 0 });
  const [filters, setFilters] = useState({ keyword: '', status: '' });
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [statusTarget, setStatusTarget] = useState(null);

  const loadUsers = useCallback((pg) => {
    setLoading(true);
    getOwnerUsers({ page: pg, size: PAGE_SIZE, ...filters })
      .then((data) => {
        setUsers(data.users?.content || []);
        setTotalPages(data.users?.totalPages || 1);
        setSummary(data.summary || { total: 0, active: 0, leave: 0, locked: 0, resigned: 0 });
      })
      .catch(() => Toast.error('직원 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadUsers(page); }, [page, loadUsers]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadUsers(0);
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      Toast.error('비밀번호 확인이 일치하지 않습니다.');
      return;
    }
    setSubmitting(true);
    try {
      await createOwnerEmployee(form);
      Toast.success('직원이 등록되었습니다.');
      setShowCreate(false);
      setForm(emptyForm);
      loadUsers(0);
      setPage(0);
    } catch {
      /* toast from interceptor */
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusSaved = (patch) => {
    setUsers((prev) => prev.map((u) => (
      u.id === statusTarget?.id
        ? {
          ...u,
          status: patch.status ?? u.status,
          dailyDepositLimit: patch.dailyDepositLimit !== undefined ? patch.dailyDepositLimit : u.dailyDepositLimit,
          todayUsedDeposit: patch.todayUsedDeposit ?? u.todayUsedDeposit,
        }
        : u
    )));
    loadUsers(page);
  };

  const handleUnlock = async (user) => {
    try {
      await unlockOwnerEmployee(user.id);
      Toast.success('잠금이 해제되었습니다.');
      loadUsers(page);
    } catch { /* */ }
  };

  const handleResetPassword = async (user) => {
    if (!window.confirm(`${user.name} 직원에게 비밀번호 초기화 메일을 발송할까요?`)) return;
    try {
      await resetOwnerEmployeePassword(user.id);
      Toast.success('초기화 메일이 발송되었습니다.');
    } catch { /* */ }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">직원 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">소속 매장 직원의 등록·계정 상태·1일 예치금 한도·비밀번호를 관리합니다.</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 text-sm font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm"
        >
          + 직원 등록
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <p className="text-xs font-semibold text-slate-500">총 직원</p>
          <p className="text-2xl font-extrabold text-slate-800 mt-1">{summary.total}명</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <p className="text-xs font-semibold text-slate-500">재직 중</p>
          <p className="text-2xl font-extrabold text-emerald-600 mt-1">{summary.active}명</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <p className="text-xs font-semibold text-slate-500">휴직</p>
          <p className="text-2xl font-extrabold text-amber-500 mt-1">{summary.leave}명</p>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <p className="text-xs font-semibold text-slate-500">잠긴 계정</p>
          <p className="text-2xl font-extrabold text-rose-600 mt-1">{summary.locked}명</p>
        </div>
      </div>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-2 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <input
          value={filters.keyword}
          onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
          placeholder="이름·이메일·연락처 검색"
          className="flex-1 min-w-[180px] px-3 py-2 text-sm border border-slate-200 rounded-xl"
        />
        <select
          value={filters.status}
          onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
          className="px-3 py-2 text-sm border border-slate-200 rounded-xl"
        >
          <option value="">전체 상태</option>
          <option value="ACTIVE">재직</option>
          <option value="ON_LEAVE">휴직</option>
          <option value="RESIGNED">퇴사</option>
        </select>
        <button type="submit" className="px-4 py-2 text-sm font-medium bg-slate-800 text-white rounded-xl">검색</button>
      </form>

      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 flex justify-center py-16"><Spinner size="lg" /></div>
      ) : users.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200"><EmptyState message="등록된 직원이 없습니다." /></div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                {['이름', '이메일', '연락처', '상태', '계정', '관리'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm font-semibold text-slate-800">{user.name}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{user.email}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{user.phone}</td>
                  <td className="px-4 py-3"><StatusBadge status={user.status} /></td>
                  <td className="px-4 py-3 text-xs font-semibold">
                    {user.isLocked ? <span className="text-rose-600">잠김</span> : <span className="text-blue-600">정상</span>}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1.5">
                      {user.isLocked && (
                        <button type="button" onClick={() => handleUnlock(user)} className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-blue-50 text-blue-700 border border-blue-100 hover:bg-blue-100">
                          잠금 해제
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setStatusTarget(user)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-100 hover:bg-indigo-100"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor" aria-hidden>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                          <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                        관리
                      </button>
                      <button type="button" onClick={() => handleResetPassword(user)} className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-amber-50 text-amber-700 border border-amber-100 hover:bg-amber-100">
                        비밀번호 초기화
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}

      {statusTarget && (
        <EmployeeStatusModal
          user={statusTarget}
          onClose={() => setStatusTarget(null)}
          onSaved={handleStatusSaved}
        />
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setShowCreate(false)} />
          <form onSubmit={handleCreate} className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 space-y-3">
            <h3 className="text-lg font-bold text-slate-800">직원 계정 생성</h3>
            {[
              { key: 'name', label: '이름', type: 'text' },
              { key: 'email', label: '이메일', type: 'email' },
              { key: 'phone', label: '연락처', type: 'text' },
              { key: 'birthday', label: '생년월일', type: 'date' },
              { key: 'password', label: '비밀번호', type: 'password' },
              { key: 'confirmPassword', label: '비밀번호 확인', type: 'password' },
            ].map((field) => (
              <label key={field.key} className="block text-sm">
                <span className="text-slate-600 font-medium">{field.label}</span>
                <input
                  required
                  type={field.type}
                  value={form[field.key]}
                  onChange={(e) => setForm((f) => ({ ...f, [field.key]: e.target.value }))}
                  className="mt-1 w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
                />
              </label>
            ))}
            <div className="flex gap-2 pt-2">
              <button type="button" onClick={() => setShowCreate(false)} className="flex-1 py-2.5 rounded-xl bg-slate-100 text-slate-600 text-sm font-medium">취소</button>
              <button type="submit" disabled={submitting} className="flex-1 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-semibold disabled:opacity-50">
                {submitting ? '등록 중…' : '등록'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
