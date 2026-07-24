import { useState, useEffect, useCallback } from 'react';
import { getOwnerDeliveryList } from '../api/deliveryOwnerApi';
import { getEmpOrderDetail } from '../api/orderEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

const TABS = [
  { key: null,        label: '전체' },
  { key: 'READY',     label: '배송 준비' },
  { key: 'SHIPPING',  label: '배송 중' },
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

function OrderDetailModal({ orderId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getEmpOrderDetail(orderId)
      .then(setDetail)
      .catch(() => { Toast.error('상세 내역을 불러오지 못했습니다.'); onClose(); })
      .finally(() => setLoading(false));
  }, [orderId, onClose]);

  if (loading) return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40"><Spinner size="lg" /></div>;
  if (!detail) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4 shrink-0">
          <h2 className="text-lg font-bold text-slate-800">발주 · 배송 상세</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>
        <div className="overflow-y-auto flex-1 pr-2 space-y-3 text-sm">
          <div className="grid grid-cols-2 gap-3 bg-slate-50 p-4 rounded-xl">
            <div><span className="text-slate-500 block text-xs">주문 번호</span><span className="font-semibold">#{detail.orderRequestId}</span></div>
            <div><span className="text-slate-500 block text-xs">신청 일시</span><span className="font-semibold">{detail.requestDate ? new Date(detail.requestDate).toLocaleString('ko-KR') : '-'}</span></div>
            <div><span className="text-slate-500 block text-xs">배송 상태</span><span className="font-bold text-emerald-600">{detail.deliveryStatus || '-'}</span></div>
            <div><span className="text-slate-500 block text-xs">운송장</span><span className="font-mono">{detail.trackingNumber || '-'}</span></div>
          </div>
          <p className="text-xs text-slate-400">배송 시작/완료 상태 변경은 본사(ADMIN)만 가능합니다.</p>
        </div>
        <div className="mt-4 pt-4 border-t border-slate-100">
          <button onClick={onClose} className="w-full py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200">닫기</button>
        </div>
      </div>
    </div>
  );
}

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
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

export default function DeliveryOwnerPage() {
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [activeTab, setActiveTab] = useState(null);
  const [selectedOrderId, setSelectedOrderId] = useState(null);

  const load = useCallback((pg, status) => {
    setLoading(true);
    getOwnerDeliveryList({ status: status || undefined, page: pg, size: PAGE_SIZE })
      .then((data) => {
        setDeliveries(data.content || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(() => { Toast.error('매장 배송 목록을 불러오지 못했습니다.'); setDeliveries([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page, activeTab); }, [page, activeTab, load]);

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? val : d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">매장 배송 현황</h2>
          <p className="text-sm text-slate-500 mt-0.5">우리 매장으로 향하는 전체 배송을 조회합니다. (상태 변경은 본사 전용)</p>
        </div>
        <button onClick={() => load(page, activeTab)} className="px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm">
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
              onClick={() => { setActiveTab(tab.key); setPage(0); }}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                isActive ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
              }`}
            >
              {meta && <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-white/70' : meta.dot}`} />}
              {tab.label}
            </button>
          );
        })}
      </div>

      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      ) : deliveries.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message="조회된 매장 배송 내역이 없습니다." />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {['주문번호', '수령인', '배송지', '배송 상태', '운송장', '발송일'].map((label) => (
                    <th key={label} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">{label}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {deliveries.map((del) => (
                  <tr key={del.deliveryId} onClick={() => setSelectedOrderId(del.orderId)} className="hover:bg-slate-50 cursor-pointer transition-colors">
                    <td className="px-5 py-3.5"><span className="text-sm font-bold text-slate-500 font-mono">#{del.orderId}</span></td>
                    <td className="px-5 py-3.5 text-sm font-semibold text-slate-800">{del.receiverName || '-'}</td>
                    <td className="px-5 py-3.5"><span className="text-sm text-slate-600 block truncate max-w-[220px]">{del.destinationAddress || '-'}</span></td>
                    <td className="px-5 py-3.5"><DeliveryStatusBadge status={del.status} /></td>
                    <td className="px-5 py-3.5">
                      {del.trackingNumber ? (
                        <span className="text-sm font-mono text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg border border-slate-200">{del.trackingNumber}</span>
                      ) : (
                        <span className="text-xs text-slate-400">등록 전</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(del.shippedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}
      {selectedOrderId && <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} />}
    </div>
  );
}
