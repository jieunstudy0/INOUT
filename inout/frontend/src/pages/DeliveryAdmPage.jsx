import { useState, useEffect, useCallback } from 'react';
import {
  getDeliveryList,
  startShipping,
  completeDelivery,
  generateWaybill,
} from '../api/deliveryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import DeliveryTrackingModal from '../components/delivery/DeliveryTrackingModal';

const PAGE_SIZE = 10;

const TABS = [
  { key: null, label: '전체' },
  { key: 'READY', label: '배송 준비' },
  { key: 'SHIPPING', label: '배송 중' },
  { key: 'COMPLETED', label: '배송 완료' },
];

const STATUS_META = {
  READY: { label: '배송 준비', dot: 'bg-amber-400', cls: 'bg-amber-50 text-amber-700 border border-amber-200' },
  SHIPPING: { label: '배송 중', dot: 'bg-blue-500', cls: 'bg-blue-50 text-blue-700 border border-blue-200' },
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
  const end = Math.min(totalPages, start + 5);
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const base = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';

  return (
    <div className="flex items-center gap-1">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${base} ${page === 0 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" /></svg>
      </button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${base} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${base} ${page >= totalPages - 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" /></svg>
      </button>
    </div>
  );
}

function TrackingInputForm({ deliveryId, orderId, initialTracking = '', onSuccess }) {
  const [value, setValue] = useState(initialTracking || '');
  const [loading, setLoading] = useState(false);
  const [issuing, setIssuing] = useState(false);

  useEffect(() => {
    if (initialTracking) setValue(initialTracking);
  }, [initialTracking]);

  const handleGenerate = async () => {
    setIssuing(true);
    try {
      const detail = await generateWaybill(deliveryId);
      setValue(detail?.trackingNumber || '');
      Toast.success('CJ대한통운 운송장이 발급되었습니다.');
      onSuccess?.();
    } catch {
      /* interceptor */
    } finally {
      setIssuing(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!value.trim()) {
      Toast.warning('운송장 번호를 입력해주세요.');
      return;
    }
    setLoading(true);
    try {
      await startShipping(orderId, value.trim());
      Toast.success('배송이 시작되었습니다.');
      onSuccess();
    } catch {
      /* interceptor */
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-1.5 min-w-[220px]">
      <div className="flex items-center gap-1.5 flex-wrap">
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="운송장 번호 입력"
          className="w-36 px-2.5 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none focus:ring-2 focus:ring-indigo-400/40 focus:border-indigo-400 transition-all font-mono"
        />
        <button
          type="button"
          onClick={handleGenerate}
          disabled={issuing || loading}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 text-[11px] font-semibold rounded-lg bg-amber-50 text-amber-800 border border-amber-200 hover:bg-amber-100 disabled:opacity-50 whitespace-nowrap"
          title="택배사 연동 Mock 자동발급"
        >
          {issuing ? (
            <>
              <Spinner size="sm" />
              발급 중…
            </>
          ) : (
            <>📦 택배사 연동 자동발급</>
          )}
        </button>
        <button
          type="submit"
          disabled={loading || issuing}
          className="px-2.5 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors whitespace-nowrap"
        >
          {loading ? '처리 중' : '발송'}
        </button>
      </div>
    </form>
  );
}

function TrackingCell({ del, onTrack, onRefresh }) {
  if (del.trackingNumber) {
    return (
      <div className="flex items-center gap-2 flex-wrap">
        <div className="flex flex-col gap-0.5">
          {del.carrier && <span className="text-[10px] text-slate-400">{del.carrier}</span>}
          <span className="text-sm font-mono text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
            {del.trackingNumber}
          </span>
        </div>
        <button
          type="button"
          onClick={() => onTrack(del)}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 text-[11px] font-semibold rounded-lg bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-100 whitespace-nowrap"
        >
          🚚 배송 조회
        </button>
      </div>
    );
  }

  if (del.status === 'READY') {
    return (
      <TrackingInputForm
        deliveryId={del.deliveryId}
        orderId={del.orderId}
        onSuccess={onRefresh}
      />
    );
  }

  return <span className="text-xs text-slate-400">-</span>;
}

function DeliveryTable({ deliveries, onRefresh, onTrack }) {
  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return Number.isNaN(d.getTime())
      ? val
      : d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  const handleComplete = async (orderId) => {
    if (!window.confirm('배송 완료 처리하시겠습니까?')) return;
    try {
      await completeDelivery(orderId);
      Toast.success('배송 완료 처리되었습니다.');
      onRefresh();
    } catch { /* */ }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {['주문번호', '수령인', '배송 상태', '운송장 번호', '등록일', '발송일', '완료일', '액션'].map((label) => (
                <th key={label} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {deliveries.map((del) => (
              <tr key={del.deliveryId} className="hover:bg-slate-50 transition-colors">
                <td className="px-5 py-3.5"><span className="text-sm font-bold text-indigo-700 font-mono">#{del.orderId}</span></td>
                <td className="px-5 py-3.5"><span className="text-sm font-medium text-slate-800">{del.receiverName}</span></td>
                <td className="px-5 py-3.5"><DeliveryStatusBadge status={del.status} /></td>
                <td className="px-5 py-3.5">
                  <TrackingCell del={del} onTrack={onTrack} onRefresh={onRefresh} />
                </td>
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.createdAt)}</td>
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.shippedAt)}</td>
                <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.deliveredAt)}</td>
                <td className="px-5 py-3.5">
                  {del.status === 'SHIPPING' && (
                    <button
                      type="button"
                      onClick={() => handleComplete(del.orderId)}
                      className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 whitespace-nowrap"
                    >
                      배송 완료
                    </button>
                  )}
                  {del.status === 'COMPLETED' && (
                    <span className="text-xs text-emerald-600 font-medium">완료</span>
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
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [activeTab, setActiveTab] = useState(null);
  const [trackingTarget, setTrackingTarget] = useState(null);

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
  const handleRefresh = () => { load(page, activeTab); };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">배송 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            운송장 자동발급·배송 조회·상태 변경
            {totalElements > 0 && (
              <span className="ml-2 text-indigo-600 font-semibold">총 {totalElements.toLocaleString()}건</span>
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={handleRefresh}
          className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm"
        >
          새로고침
        </button>
      </div>

      <div className="flex flex-wrap gap-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-1.5">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.key;
          const meta = tab.key ? STATUS_META[tab.key] : null;
          return (
            <button
              key={String(tab.key)}
              type="button"
              onClick={() => handleTabChange(tab.key)}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                isActive ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100'
              }`}
            >
              {meta && <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-white/70' : meta.dot}`} />}
              {tab.label}
            </button>
          );
        })}
      </div>

      {loading && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20 gap-3 text-slate-400">
          <Spinner size="lg" /><span className="text-sm">배송 데이터를 불러오는 중...</span>
        </div>
      )}

      {!loading && deliveries.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message={activeTab ? `'${STATUS_META[activeTab]?.label}' 상태의 배송 내역이 없습니다.` : '처리할 배송 내역이 없습니다.'} />
        </div>
      )}

      {!loading && deliveries.length > 0 && (
        <DeliveryTable
          deliveries={deliveries}
          onRefresh={handleRefresh}
          onTrack={(del) => setTrackingTarget(del)}
        />
      )}

      {!loading && totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination page={page} totalPages={totalPages} onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
        </div>
      )}

      <DeliveryTrackingModal
        open={!!trackingTarget}
        onClose={() => setTrackingTarget(null)}
        carrier={trackingTarget?.carrier}
        trackingNumber={trackingTarget?.trackingNumber}
      />
    </div>
  );
}
