import { useState, useEffect, useCallback } from 'react';
import { getDeliveryList, startShipping, completeDelivery } from '../api/deliveryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

const TABS = [
  { key: null,        label: '전체'     },
  { key: 'READY',     label: '배송 준비' },
  { key: 'SHIPPING',  label: '배송 중'  },
  { key: 'COMPLETED', label: '배송 완료' },
];

const STATUS_META = {
  READY:     { label: '배송 준비', dot: 'bg-amber-400',   cls: 'bg-amber-50  text-amber-700  border border-amber-200'   },
  SHIPPING:  { label: '배송 중',   dot: 'bg-blue-500',    cls: 'bg-blue-50   text-blue-700   border border-blue-200'    },
  COMPLETED: { label: '배송 완료', dot: 'bg-emerald-500', cls: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
};


function DeliveryStatusBadge({ status }) {
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
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const base = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';

  return (
    <div className="flex items-center gap-1">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)}
        className={`${base} ${page === 0 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
      </button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)}
          className={`${base} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>
          {p + 1}
        </button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}
        className={`${base} ${page >= totalPages - 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
        </svg>
      </button>
    </div>
  );
}

function TrackingInputForm({ orderId, onSuccess }) {
  const [value, setValue]       = useState('');
  const [loading, setLoading]   = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!value.trim()) { Toast.warning('운송장 번호를 입력해주세요.'); return; }
    setLoading(true);
    try {
      await startShipping(orderId, value.trim());
      Toast.success('배송이 시작되었습니다.');
      onSuccess();
    } catch {
      /* apiClient 인터셉터가 Toast 처리 */
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-1.5">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="운송장 번호 입력"
        className="w-36 px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none focus:ring-2 focus:ring-indigo-400/40 focus:border-indigo-400 transition-all"
      />
      <button type="submit" disabled={loading}
        className="px-2.5 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors whitespace-nowrap">
        {loading ? '처리 중' : '발송'}
      </button>
    </form>
  );
}


function DeliveryTable({ deliveries, onRefresh }) {
  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  const handleComplete = async (orderId) => {
    if (!window.confirm('배송 완료 처리하시겠습니까?')) return;
    try {
      await completeDelivery(orderId);
      Toast.success('배송 완료 처리되었습니다.');
      onRefresh();
    } catch {}
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {[
                { label: '주문번호',   cls: 'text-left'   },
                { label: '수령인',     cls: 'text-left'   },
                { label: '배송 상태',  cls: 'text-center' },
                { label: '운송장 번호', cls: 'text-left'  },
                { label: '등록일',     cls: 'text-left'   },
                { label: '발송일',     cls: 'text-left'   },
                { label: '완료일',     cls: 'text-left'   },
                { label: '액션',       cls: 'text-center' },
              ].map(({ label, cls }) => (
                <th key={label}
                  className={`px-5 py-3 ${cls} text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap`}>
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {deliveries.map((del) => (
              <tr key={del.deliveryId} className="hover:bg-slate-50 transition-colors">
                {/* 주문번호 */}
                <td className="px-5 py-3.5">
                  <span className="text-sm font-bold text-indigo-700 font-mono">#{del.orderId}</span>
                </td>

                {/* 수령인 */}
                <td className="px-5 py-3.5">
                  <span className="text-sm font-medium text-slate-800">{del.receiverName}</span>
                </td>

                {/* 배송 상태 */}
                <td className="px-5 py-3.5 text-center">
                  <DeliveryStatusBadge status={del.status} />
                </td>

                {/* 운송장 번호 */}
                <td className="px-5 py-3.5">
                  {del.trackingNumber ? (
                    <span className="text-sm font-mono text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                      {del.trackingNumber}
                    </span>
                  ) : del.status === 'READY' ? (
                    /* 실제 입력 폼 — READY 상태일 때만 활성화 */
                    <TrackingInputForm orderId={del.orderId} onSuccess={onRefresh} />
                  ) : (
                    /* 스켈레톤 버튼 — 추후 기능 확장용 플레이스홀더 */
                    <button
                      disabled
                      className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg border border-dashed border-slate-300 text-slate-400 cursor-not-allowed bg-slate-50">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                      </svg>
                      입력하기
                    </button>
                  )}
                </td>

                {/* 날짜 컬럼 */}
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.createdAt)}</td>
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.shippedAt)}</td>
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.deliveredAt)}</td>

                {/* 액션 */}
                <td className="px-5 py-3.5 text-center">
                  {del.status === 'SHIPPING' && (
                    <button onClick={() => handleComplete(del.orderId)}
                      className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 transition-colors whitespace-nowrap">
                      배송 완료
                    </button>
                  )}
                  {del.status === 'COMPLETED' && (
                    <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-medium">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      완료
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function DeliveryAdmPage() {
  const [deliveries, setDeliveries]     = useState([]);
  const [loading, setLoading]           = useState(true);
  const [page, setPage]                 = useState(0);
  const [totalPages, setTotalPages]     = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [activeTab, setActiveTab]       = useState(null); // null = 전체

  const load = useCallback((pg, status) => {
    setLoading(true);
    getDeliveryList({ status: status || undefined, page: pg, size: PAGE_SIZE })
      .then((data) => {
        setDeliveries(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
      })
      .catch(() => { Toast.error('배송 목록을 불러오지 못했습니다.'); setDeliveries([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page, activeTab); }, [page, activeTab, load]);

  const handleTabChange = (key) => { setActiveTab(key); setPage(0); };
  const handleRefresh   = () => { load(page, activeTab); };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">

      {/* ── 헤더 ── */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">배송 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            승인된 발주의 운송장 등록 및 배송 상태 변경
            {totalElements > 0 && (
              <span className="ml-2 text-indigo-600 font-semibold">총 {totalElements.toLocaleString()}건</span>
            )}
          </p>
        </div>
        <button onClick={handleRefresh}
          className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
          새로고침
        </button>
      </div>

      {/* ── 탭 필터 ── */}
      <div className="flex flex-wrap gap-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-1.5">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.key;
          const meta = tab.key ? STATUS_META[tab.key] : null;
          return (
            <button key={String(tab.key)} onClick={() => handleTabChange(tab.key)}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
              }`}>
              {meta && (
                <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-white/70' : meta.dot}`} />
              )}
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── 콘텐츠 ── */}
      {loading && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20 gap-3 text-slate-400">
          <Spinner size="lg" /><span className="text-sm">배송 데이터를 불러오는 중...</span>
        </div>
      )}

      {!loading && deliveries.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message={
            activeTab
              ? `'${STATUS_META[activeTab]?.label}' 상태의 배송 내역이 없습니다.`
              : '처리할 배송 내역이 없습니다.'
          } />
        </div>
      )}

      {!loading && deliveries.length > 0 && (
        <DeliveryTable deliveries={deliveries} onRefresh={handleRefresh} />
      )}

      {/* ── 페이지네이션 ── */}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination page={page} totalPages={totalPages}
            onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
        </div>
      )}
    </div>
  );
}
