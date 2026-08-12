import { useState, useEffect, useCallback } from 'react';
import { getVacationList, processVacation } from '../api/vacationOwnerApi.js';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

const TABS = [
  { key: null,        label: '전체'   },
  { key: 'PENDING',   label: '대기'   },
  { key: 'APPROVED',  label: '승인'   },
  { key: 'REJECTED',  label: '반려'   },
  { key: 'HOLD',      label: '보류'   },
];

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
    <div className="flex items-center gap-1">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${base} ${page === 0 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${base} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${base} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

// 반려 사유 입력 모달 — 승인/보류와 달리 사유 입력이 필수이므로 별도 모달로 분리
function RejectModal({ leave, onClose, onConfirm, submitting }) {
  const [rejectReason, setRejectReason] = useState('');

  const handleConfirm = () => {
    if (!rejectReason.trim()) {
      window.alert('반려 사유를 입력해주세요.');
      return;
    }
    onConfirm(rejectReason.trim());
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <h3 className="text-lg font-bold text-slate-800 mb-1">연차 신청 반려</h3>
        <p className="text-sm text-slate-500 mb-4">
          {leave.employeeName}님의 {leave.startDate} ~ {leave.endDate} 연차 신청을 반려합니다. 반려 사유를 입력해주세요.
        </p>
        <textarea
          autoFocus
          rows="4"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          maxLength={500}
          placeholder="반려 사유를 입력하세요."
          className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-rose-400/40 focus:border-rose-400 focus:bg-white transition-all resize-none"
        />
        <div className="flex gap-2 mt-4">
          <button type="button" onClick={onClose} disabled={submitting} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 disabled:opacity-50">
            취소
          </button>
          <button type="button" onClick={handleConfirm} disabled={submitting} className="flex-1 py-2.5 text-sm font-bold bg-rose-600 text-white rounded-xl hover:bg-rose-700 disabled:opacity-50">
            {submitting ? '처리 중...' : '반려 처리'}
          </button>
        </div>
      </div>
    </div>
  );
}

function VacationTable({ leaves, processingId, onApprove, onHold, onOpenReject }) {
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
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {['신청자', '연차기간', '종류', '신청일자', '상태', '처리'].map((label) => (
                <th key={label} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {leaves.map((leave) => {
              const isProcessing = processingId === leave.leaveId;
              const isPending = leave.status === 'PENDING' || leave.status === 'HOLD';
              return (
                <tr key={leave.leaveId} className="hover:bg-slate-50 transition-colors">
                  <td className="px-5 py-3.5"><span className="text-sm font-semibold text-slate-800">{leave.employeeName}</span></td>
                  <td className="px-5 py-3.5"><span className="text-sm text-slate-700 whitespace-nowrap">{formatDate(leave.startDate)} ~ {formatDate(leave.endDate)}</span></td>
                  <td className="px-5 py-3.5"><span className="text-sm text-slate-600">{TYPE_LABEL[leave.type] || leave.type}</span></td>
                  <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDateTime(leave.createdAt)}</td>
                  <td className="px-5 py-3.5"><LeaveStatusBadge status={leave.status} /></td>
                  <td className="px-5 py-3.5">
                    {isPending ? (
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <button disabled={!!processingId} onClick={() => onApprove(leave)}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 disabled:opacity-50 transition-colors">
                          {isProcessing ? '...' : '승인'}
                        </button>
                        <button disabled={!!processingId} onClick={() => onOpenReject(leave)}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 disabled:opacity-50 transition-colors">
                          반려
                        </button>
                        {leave.status !== 'HOLD' && (
                          <button disabled={!!processingId} onClick={() => onHold(leave)}
                            className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-slate-100 text-slate-600 border border-slate-200 hover:bg-slate-200 disabled:opacity-50 transition-colors">
                            보류
                          </button>
                        )}
                      </div>
                    ) : (
                      <span className="text-xs text-slate-400">처리 완료</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function VacationOwnerPage() {
  const [leaves, setLeaves]         = useState([]);
  const [loading, setLoading]       = useState(true);
  const [page, setPage]             = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [activeTab, setActiveTab]   = useState('PENDING');
  const [processingId, setProcessingId] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);

  const load = useCallback((pg, status) => {
    setLoading(true);
    getVacationList({ status: status || undefined, page: pg, size: PAGE_SIZE })
      .then((data) => {
        setLeaves(data.content || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(() => { Toast.error('연차 신청 목록을 불러오지 못했습니다.'); setLeaves([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page, activeTab); }, [page, activeTab, load]);

  const handleTabChange = (key) => { setActiveTab(key); setPage(0); };
  const handleRefresh   = () => load(page, activeTab);

  // 승인/반려 처리 후에는 목록을 비동기로 다시 조회해, 새로고침 없이 화면 상태를 즉시 갱신한다.
  const runProcess = async (leaveId, payload, successMessage) => {
    setProcessingId(leaveId);
    try {
      await processVacation(leaveId, payload);
      Toast.success(successMessage);
      load(page, activeTab);
      setRejectTarget(null);
    } catch (error) {
      // Toast 에러는 apiClient 인터셉터에서 처리됨
    } finally {
      setProcessingId(null);
    }
  };

  const handleApprove = (leave) => {
    if (!window.confirm(`${leave.employeeName}님의 연차 신청을 승인하시겠습니까?`)) return;
    runProcess(leave.leaveId, { status: 'APPROVED' }, '연차 신청이 승인 완료되었습니다.');
  };

  const handleHold = (leave) => {
    runProcess(leave.leaveId, { status: 'HOLD' }, '연차 신청이 보류 처리되었습니다.');
  };

  const handleRejectConfirm = (rejectReason) => {
    runProcess(rejectTarget.leaveId, { status: 'REJECTED', rejectReason }, '연차 신청이 반려되었습니다.');
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">연차 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">직원의 연차(휴가) 신청을 심사하고 승인·반려·보류 처리합니다.</p>
        </div>
        <button onClick={handleRefresh} className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
          🔄 새로고침
        </button>
      </div>

      <div className="flex flex-wrap gap-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-1.5 w-fit">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.key;
          const meta = tab.key ? STATUS_META[tab.key] : null;
          return (
            <button key={String(tab.key)} onClick={() => handleTabChange(tab.key)}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                isActive ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
              }`}>
              {meta && <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-white/70' : meta.dot}`} />}
              {tab.label}
            </button>
          );
        })}
      </div>

      {loading && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20 gap-3 text-slate-400">
          <Spinner size="lg" /><span className="text-sm">연차 신청 목록을 불러오는 중...</span>
        </div>
      )}

      {!loading && leaves.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message={activeTab ? `'${STATUS_META[activeTab]?.label}' 상태의 연차 신청이 없습니다.` : '연차 신청 내역이 없습니다.'} />
        </div>
      )}

      {!loading && leaves.length > 0 && (
        <VacationTable
          leaves={leaves}
          processingId={processingId}
          onApprove={handleApprove}
          onHold={handleHold}
          onOpenReject={setRejectTarget}
        />
      )}

      {!loading && totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </div>
      )}

      {rejectTarget && (
        <RejectModal
          leave={rejectTarget}
          submitting={processingId === rejectTarget.leaveId}
          onClose={() => setRejectTarget(null)}
          onConfirm={handleRejectConfirm}
        />
      )}
    </div>
  );
}
