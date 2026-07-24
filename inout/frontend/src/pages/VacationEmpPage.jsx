import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyLeaveList } from '../api/vacationEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { useAppBasePath } from '../utils/appPaths';

const PAGE_SIZE = 10;

const TYPE_LABEL = {
  ANNUAL:   '연차',
  HALF_DAY: '반차',
  SICK:     '병가',
};

const STATUS_META = {
  PENDING:  { label: '대기', dot: 'bg-amber-400',   cls: 'bg-amber-50  text-amber-700  border border-amber-200'   },
  APPROVED: { label: '승인', dot: 'bg-emerald-500', cls: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
  REJECTED: { label: '반려', dot: 'bg-rose-500',    cls: 'bg-rose-50   text-rose-700   border border-rose-200'    },
  HOLD:     { label: '보류', dot: 'bg-slate-400',   cls: 'bg-slate-100 text-slate-600  border border-slate-200'   },
};

function LeaveStatusBadge({ status }) {
  const meta = STATUS_META[status] || { label: status, dot: 'bg-slate-400', cls: 'bg-slate-100 text-slate-600' };
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold ${meta.cls}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${meta.dot} inline-block`} />
      {meta.label}
    </span>
  );
}

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end   = Math.min(totalPages, start + 5);
  const pages = Array.from({ length: end - start }, (_, i) => start + i);
  const base = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';

  return (
    <div className="flex items-center gap-1 mt-6 justify-center">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${base} ${page === 0 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${base} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${base} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

export default function VacationEmpPage() {
  const navigate = useNavigate();
  const base = useAppBasePath();
  const leaveRoot = base === '/owner' ? '/owner/vacation/my' : '/emp/vacation';
  const leaveNew = base === '/owner' ? '/owner/vacation/new' : '/emp/vacation/new';
  const [leaves, setLeaves]         = useState([]);
  const [loading, setLoading]       = useState(true);
  const [page, setPage]             = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const load = useCallback((pg) => {
    setLoading(true);
    getMyLeaveList({ page: pg, size: PAGE_SIZE })
      .then((data) => {
        setLeaves(data.content || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(() => { Toast.error('연차 신청 목록을 불러오지 못했습니다.'); setLeaves([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page); }, [page, load]);

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleDateString('ko-KR');
  };

  const formatDateTime = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">연차 신청 내역</h2>
          <p className="text-sm text-slate-500 mt-0.5">본인이 신청한 연차(휴가)의 처리 상태를 확인합니다.</p>
        </div>
        <button
          onClick={() => navigate(leaveNew)}
          className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition-all shadow-sm"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          연차 신청
        </button>
      </div>

      {/* 콘텐츠 */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      ) : leaves.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message="신청한 연차 내역이 없습니다." />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {['신청일자', '연차기간', '종류', '상태'].map((label) => (
                    <th key={label} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {leaves.map((leave) => (
                  <tr key={leave.leaveId} onClick={() => navigate(`${leaveRoot}/${leave.leaveId}`)} className="hover:bg-slate-50 cursor-pointer transition-colors group">
                    <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDateTime(leave.createdAt)}</td>
                    <td className="px-5 py-3.5">
                      <span className="text-sm font-semibold text-slate-800 group-hover:text-indigo-600 whitespace-nowrap">
                        {formatDate(leave.startDate)} ~ {formatDate(leave.endDate)}
                      </span>
                    </td>
                    <td className="px-5 py-3.5"><span className="text-sm text-slate-600">{TYPE_LABEL[leave.type] || leave.type}</span></td>
                    <td className="px-5 py-3.5"><LeaveStatusBadge status={leave.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 페이지네이션 */}
      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}
    </div>
  );
}
