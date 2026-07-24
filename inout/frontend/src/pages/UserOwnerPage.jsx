import { useState, useEffect, useCallback } from 'react';
import {
  getOwnerUsers,
  createOwnerEmployee,
  updateOwnerEmployee,
  unlockOwnerEmployee,
  resetOwnerEmployeePassword,
} from '../api/ownerUserApi.js';
import { Toast } from '../utils/toast.js';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

function StatusBadge({ status }) {
  const config = {
    ACTIVE:   { label: '재직 중', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    LEAVE:    { label: '휴직',    cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    RESIGNED: { label: '퇴사',    cls: 'bg-slate-100 text-slate-500 border-slate-200' },
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

  const handleResign = async (user) => {
    if (!window.confirm(`${user.name} 직원을 퇴사 처리하시겠습니까?`)) return;
    try {
      await updateOwnerEmployee(user.id, { status: 'RESIGNED' });
      Toast.success('퇴사 처리되었습니다.');
      loadUsers(page);
    } catch { /* */ }
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
          <p className="text-sm text-slate-500 mt-0.5">소속 매장 직원 등록, 퇴사, 잠금 해제, 비밀번호 초기화를 관리합니다.</p>
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
          <option value="LEAVE">휴직</option>
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
                        <button onClick={() => handleUnlock(user)} className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-blue-50 text-blue-700 border border-blue-100 hover:bg-blue-100">
                          잠금 해제
                        </button>
                      )}
                      {user.status !== 'RESIGNED' && (
                        <button onClick={() => handleResign(user)} className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-slate-50 text-slate-600 border border-slate-200 hover:bg-slate-100">
                          퇴사
                        </button>
                      )}
                      <button onClick={() => handleResetPassword(user)} className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-amber-50 text-amber-700 border border-amber-100 hover:bg-amber-100">
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
