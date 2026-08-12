import { useState, useEffect, useCallback } from 'react';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import UserDetailModal from '../components/admin/UserDetailModal';
import PersonName from '../components/common/PersonName';
import { getUserList } from '../api/adminUserApi';

const PAGE_SIZE = 10;

function StatusBadge({ status }) {
  const config = {
    ACTIVE: { label: '재직 중', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    ON_LEAVE: { label: '휴직', cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    LEAVE: { label: '휴직', cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    RESIGNED: { label: '퇴사', cls: 'bg-slate-100 text-slate-500 border-slate-200' },
  }[status] || { label: status, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${config.cls}`}>{config.label}</span>;
}

function AccountBadge({ isLocked }) {
  if (isLocked) {
    return (
      <span className="flex items-center gap-1 text-rose-600 text-[11px] font-bold bg-rose-50 px-2 py-1 rounded border border-rose-100">
        <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
        </svg>
        잠김
      </span>
    );
  }
  return <span className="text-blue-600 text-[11px] font-bold bg-blue-50 px-2 py-1 rounded border border-blue-100">정상</span>;
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
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${btnBase} ${page === 0 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${btnBase} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${btnBase} ${page >= totalPages - 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

export default function UserAdmPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedUser, setSelectedUser] = useState(null);
  const [summary, setSummary] = useState({ total: 0, active: 0, leave: 0, locked: 0 });
  const [filters, setFilters] = useState({ keyword: '', status: '', storeId: '' });

  const loadUsers = useCallback((pg) => {
    setLoading(true);
    getUserList({ page: pg, size: PAGE_SIZE, ...filters })
      .then((data) => {
        setUsers(data.users.content);
        setTotalPages(data.users.totalPages);
        setSummary(data.summary);
        setSelectedUser((prev) => {
          if (!prev) return null;
          const fresh = data.users.content.find((u) => u.id === prev.id);
          return fresh ? { ...prev, ...fresh } : prev;
        });
      })
      .catch(() => Toast.error('회원 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadUsers(page); }, [page, loadUsers]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadUsers(0);
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">회원 관리</h2>
        <p className="text-sm text-slate-500 mt-0.5">시스템에 등록된 전체 직원의 계정 상태와 권한을 관리합니다.</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">총 직원</p><p className="text-2xl font-extrabold text-slate-800 mt-1">{summary.total}명</p></div>
          <div className="w-10 h-10 rounded-full bg-slate-50 flex items-center justify-center text-slate-400">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" /></svg>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">재직 중</p><p className="text-2xl font-extrabold text-emerald-600 mt-1">{summary.active}명</p></div>
          <div className="w-10 h-10 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">휴직</p><p className="text-2xl font-extrabold text-amber-500 mt-1">{summary.leave}명</p></div>
          <div className="w-10 h-10 rounded-full bg-amber-50 flex items-center justify-center text-amber-500">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M14.25 9v6m-4.5 0V9M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-rose-200 shadow-sm flex items-center justify-between bg-rose-50/30">
          <div><p className="text-xs font-semibold text-rose-600">잠긴 계정</p><p className="text-2xl font-extrabold text-rose-600 mt-1">{summary.locked}건</p></div>
          <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center text-rose-500">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" /></svg>
          </div>
        </div>
      </div>

      <form onSubmit={handleSearch} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap gap-3 items-center">
        <select value={filters.storeId} onChange={(e) => setFilters({ ...filters, storeId: e.target.value })} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[140px]">
          <option value="">모든 매장</option>
          <option value="1">본점</option>
          <option value="2">강남 1호점</option>
          <option value="3">홍대 2호점</option>
          <option value="0">소속 없음(미지정)</option>
        </select>
        <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[120px]">
          <option value="">모든 상태</option>
          <option value="ACTIVE">재직 중</option>
          <option value="ON_LEAVE">휴직</option>
          <option value="RESIGNED">퇴사</option>
        </select>
        <div className="flex-1 flex min-w-[200px] relative">
          <input
            type="text"
            placeholder="이름, 이메일, 연락처 검색"
            value={filters.keyword}
            onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
            className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        </div>
        <button type="submit" className="px-5 py-2 bg-slate-800 text-white text-sm font-bold rounded-xl hover:bg-slate-900 transition-colors">검색</button>
      </form>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : users.length === 0 ? (
        <EmptyState message="조건에 맞는 직원이 없습니다." />
      ) : (
        <>
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    {['회원 정보', '소속 매장', '상태', '계정 잠금', '가입일'].map((h) => (
                      <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase">{h}</th>
                    ))}
                    <th className="px-5 py-3 text-center text-[11px] font-semibold text-slate-500 uppercase">관리</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {users.map((user) => (
                    <tr key={user.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold ${user.isAdmin ? 'bg-indigo-600' : 'bg-slate-400'}`}>
                            {user.name.charAt(0)}
                          </div>
                          <div>
                            <p className="text-sm font-bold text-slate-800 flex items-center gap-1.5">
                              <PersonName name={user.name} className="font-bold text-slate-800" />
                              {user.isAdmin && <span className="bg-indigo-100 text-indigo-700 text-[9px] px-1 rounded font-bold uppercase tracking-wider">Admin</span>}
                            </p>
                            <p className="text-[11px] text-slate-400">{user.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4 text-sm font-medium text-slate-600">
                        {user.storeName || <span className="text-slate-400 text-xs">본점 (소속 없음)</span>}
                      </td>
                      <td className="px-5 py-4"><StatusBadge status={user.status} /></td>
                      <td className="px-5 py-4"><AccountBadge isLocked={user.isLocked} /></td>
                      <td className="px-5 py-4 text-xs text-slate-500">
                        {new Date(user.createdAt).toLocaleDateString('ko-KR')}
                      </td>
                      <td className="px-5 py-4 text-center">
                        <button
                          type="button"
                          onClick={() => setSelectedUser(user)}
                          className="px-3 py-1.5 border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-100 hover:border-slate-300 transition-all shadow-sm"
                        >
                          관리
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div className="flex justify-center">
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </div>
        </>
      )}

      {selectedUser && (
        <UserDetailModal
          user={selectedUser}
          onClose={() => setSelectedUser(null)}
          onRefresh={() => loadUsers(page)}
          onUserPatched={(patched) => {
            setSelectedUser(patched);
            setUsers((prev) => prev.map((u) => (u.id === patched.id ? { ...u, ...patched } : u)));
            setSummary((s) => ({
              ...s,
              locked: Math.max(0, (s.locked || 0) - (selectedUser.isLocked && !patched.isLocked ? 1 : 0)),
            }));
          }}
        />
      )}
    </div>
  );
}
