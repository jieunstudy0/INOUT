import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMyLeaveDetail } from '../api/vacationEmpApi.js';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { useAppBasePath } from '../utils/appPaths';

const TYPE_LABEL = {
  ANNUAL:   '연차',
  HALF_DAY: '반차',
  SICK:     '병가',
};

function LeaveStatusBadge({ status }) {
  const config = {
    PENDING:  { label: '대기', cls: 'bg-amber-100 text-amber-700' },
    APPROVED: { label: '승인', cls: 'bg-emerald-100 text-emerald-700' },
    REJECTED: { label: '반려', cls: 'bg-rose-100 text-rose-700' },
    HOLD:     { label: '보류', cls: 'bg-slate-100 text-slate-600' },
  }[status] || { label: status, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${config.cls}`}>{config.label}</span>;
}

export default function VacationEmpDetailPage() {
  const { id, leaveId: leaveIdParam } = useParams();
  const leaveId = id || leaveIdParam;
  const navigate = useNavigate();
  const base = useAppBasePath();
  const leaveRoot = base === '/owner' ? '/owner/leaves' : '/emp/leaves';
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadDetail = useCallback(() => {
    setLoading(true);
    getMyLeaveDetail(leaveId)
      .then(setDetail)
      .catch(() => {
        Toast.error('연차 신청 상세 내역을 불러오지 못했습니다.');
        navigate(leaveRoot);
      })
      .finally(() => setLoading(false));
  }, [leaveId, navigate, leaveRoot]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleDateString('ko-KR');
  };

  const formatDateTime = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR');
  };

  if (loading) return <div className="flex justify-center py-32"><Spinner size="lg" /></div>;
  if (!detail) return <EmptyState message="연차 신청 정보를 찾을 수 없습니다." />;

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate(leaveRoot)} className="text-slate-400 hover:text-indigo-600 p-2 bg-white rounded-full shadow-sm border border-slate-200">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div>
          <h2 className="text-xl font-bold text-slate-800">연차 신청 상세</h2>
          <p className="text-sm text-slate-500 mt-0.5">#{detail.leaveId} 연차 신청의 처리 상태를 확인합니다.</p>
        </div>
        <div className="ml-auto">
          <LeaveStatusBadge status={detail.status} />
        </div>
      </div>

      {/* 정보 카드 */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <h3 className="text-sm font-bold text-slate-800 border-b border-slate-100 pb-2">신청 정보</h3>
        <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          <div><span className="text-slate-500 block text-xs">신청 일시</span><span className="font-semibold text-slate-700">{formatDateTime(detail.createdAt)}</span></div>
          <div><span className="text-slate-500 block text-xs">연차 종류</span><span className="font-semibold text-slate-700">{TYPE_LABEL[detail.type] || detail.type}</span></div>
          <div className="col-span-2"><span className="text-slate-500 block text-xs">연차 기간</span><span className="text-lg font-bold text-indigo-600">{formatDate(detail.startDate)} ~ {formatDate(detail.endDate)}</span></div>
          <div className="col-span-2"><span className="text-slate-500 block text-xs">사유</span><span className="font-medium text-slate-700 whitespace-pre-wrap">{detail.reason || '-'}</span></div>
        </div>

        {detail.status === 'REJECTED' && detail.rejectReason && (
          <div className="p-3 bg-rose-50 rounded-lg border border-rose-100 text-sm text-rose-700">
            <span className="font-semibold">반려 사유: </span>{detail.rejectReason}
          </div>
        )}

        {detail.status !== 'PENDING' && detail.processedAt && (
          <div className="text-xs text-slate-400 pt-1">
            {formatDateTime(detail.processedAt)}에 {detail.processorName ? `${detail.processorName}님이 ` : ''}처리했습니다.
          </div>
        )}
      </div>
    </div>
  );
}
