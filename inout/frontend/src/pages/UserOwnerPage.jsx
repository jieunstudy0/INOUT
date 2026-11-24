import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOwnerUsers } from '../api/ownerUserApi.js';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import EmployeeCard from '../components/common/EmployeeCard';

const PAGE_SIZE = 12;

/** 점주 — 직원 관리. 직원을 누르면 상세 화면에서 사진·정보·상태·예치금 한도를 고친다. */
export default function UserOwnerPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState({ total: 0, active: 0, leave: 0, locked: 0, resigned: 0 });
  const [filters, setFilters] = useState({ keyword: '', status: '' });

  const loadUsers = useCallback((pg) => {
    setLoading(true);
    getOwnerUsers({ page: pg, size: PAGE_SIZE, ...filters })
      .then((data) => {
        setUsers(data.users?.content || []);
        setTotalPages(data.users?.totalPages || 1);
        setSummary(data.summary || { total: 0, active: 0, leave: 0, locked: 0, resigned: 0 });
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadUsers(page); }, [page, loadUsers]);

  const kpi = [
    { label: '총 직원', value: summary.total, cls: 'text-slate-900' },
    { label: '재직 중', value: summary.active, cls: 'text-emerald-600' },
    { label: '휴직', value: summary.leave, cls: 'text-amber-600' },
    { label: '잠긴 계정', value: summary.locked, cls: 'text-rose-600' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-3xl font-normal text-slate-900">직원 관리</h2>
          <p className="mt-2 text-slate-600">우리 매장 직원의 사진·연락처·재직 상태·1일 예치금 한도를 관리합니다.</p>
        </div>
        <button onClick={() => navigate('/owner/users/new')} className="px-5 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700">
          직원 등록
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpi.map((k) => (
          <div key={k.label} className="rounded-[10px] bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-600">{k.label}</p>
            <p className={`mt-1 text-2xl font-semibold ${k.cls}`}>{k.value}명</p>
          </div>
        ))}
      </div>

      <form onSubmit={(e) => { e.preventDefault(); setPage(0); loadUsers(0); }} className="flex flex-wrap gap-2">
        <input value={filters.keyword} onChange={(e) => setFilters((f) => ({ ...f, keyword: e.target.value }))}
          placeholder="이름·이메일·연락처 검색" className="flex-1 min-w-[200px] px-3 py-2.5 text-sm border border-slate-300 rounded-[10px]" />
        <select value={filters.status} onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
          className="px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] bg-white">
          <option value="">전체 상태</option>
          <option value="ACTIVE">재직</option>
          <option value="ON_LEAVE">휴직</option>
          <option value="RESIGNED">퇴사</option>
        </select>
        <button type="submit" className="px-6 py-2.5 text-sm rounded-full bg-indigo-600 text-white hover:bg-indigo-700">검색</button>
      </form>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : users.length === 0 ? (
        <EmptyState message="등록된 직원이 없습니다." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {users.map((u) => (
            <EmployeeCard key={u.id} user={u} onClick={() => navigate(`/owner/users/${u.id}`)}
              extra={u.dailyDepositLimit != null ? `1일 예치금 한도 ${u.dailyDepositLimit.toLocaleString('ko-KR')}원` : '1일 예치금 한도 없음'} />
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex justify-center gap-1">
          {Array.from({ length: totalPages }, (_, p) => (
            <button key={p} onClick={() => setPage(p)}
              className={`w-9 h-9 rounded-full text-sm ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-700 hover:bg-slate-50'}`}>{p + 1}</button>
          ))}
        </div>
      )}
    </div>
  );
}
