import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import EmployeeCard from '../components/common/EmployeeCard';
import { getUserList, getStores } from '../api/adminUserApi';

const PAGE_SIZE = 12;

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const btn = 'w-9 h-9 flex items-center justify-center rounded-full text-sm';
  return (
    <div className="flex items-center justify-center gap-1">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${btn} ${page === 0 ? 'text-slate-300' : 'text-slate-600 hover:bg-slate-50'}`}>‹</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${btn} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-700 hover:bg-slate-50'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${btn} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-600 hover:bg-slate-50'}`}>›</button>
    </div>
  );
}

/** 본사 — 회원 관리. 회원을 누르면 상세 화면에서 사진·정보·소속·상태·권한을 고친다. */
export default function UserAdmPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState({ total: 0, active: 0, leave: 0, locked: 0 });
  const [filters, setFilters] = useState({ keyword: '', status: '', storeId: '' });

  const loadUsers = useCallback((pg) => {
    setLoading(true);
    getUserList({ page: pg, size: PAGE_SIZE, ...filters })
      .then((data) => {
        setUsers(data.users.content);
        setTotalPages(data.users.totalPages);
        setSummary(data.summary);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadUsers(page); }, [page, loadUsers]);
  useEffect(() => { getStores().then((s) => setStores(s || [])).catch(() => {}); }, []);

  const kpi = [
    { label: '총 회원', value: `${summary.total}명`, cls: 'text-slate-900' },
    { label: '재직 중', value: `${summary.active}명`, cls: 'text-emerald-600' },
    { label: '휴직', value: `${summary.leave}명`, cls: 'text-amber-600' },
    { label: '잠긴 계정', value: `${summary.locked}건`, cls: 'text-rose-600' },
  ];
  const selectCls = 'px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] bg-white';

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-normal text-slate-900">회원 관리</h2>
        <p className="mt-2 text-slate-600">전체 직원의 사진·연락처·소속 매장·재직 상태·권한을 관리합니다. 회원을 누르면 상세 화면으로 이동합니다.</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpi.map((k) => (
          <div key={k.label} className="rounded-[10px] bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-600">{k.label}</p>
            <p className={`mt-1 text-2xl font-semibold ${k.cls}`}>{k.value}</p>
          </div>
        ))}
      </div>

      <form onSubmit={(e) => { e.preventDefault(); setPage(0); loadUsers(0); }} className="flex flex-wrap gap-2">
        <select value={filters.storeId} onChange={(e) => setFilters({ ...filters, storeId: e.target.value })} className={selectCls}>
          <option value="">모든 매장</option>
          {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          <option value="0">소속 없음</option>
        </select>
        <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })} className={selectCls}>
          <option value="">모든 상태</option>
          <option value="ACTIVE">재직 중</option>
          <option value="ON_LEAVE">휴직</option>
          <option value="RESIGNED">퇴사</option>
        </select>
        <input value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
          placeholder="이름, 이메일, 연락처 검색" className={`${selectCls} flex-1 min-w-[200px]`} />
        <button type="submit" className="px-6 py-2.5 text-sm rounded-full bg-indigo-600 text-white hover:bg-indigo-700">검색</button>
      </form>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : users.length === 0 ? (
        <EmptyState message="조건에 맞는 회원이 없습니다." />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {users.map((u) => (
              <EmployeeCard key={u.id} user={u} onClick={() => navigate(`/admin/users/${u.id}`)}
                extra={`가입 ${new Date(u.createdAt).toLocaleDateString('ko-KR')}`} />
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
