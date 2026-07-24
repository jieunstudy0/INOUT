import { useState, useEffect, useCallback } from 'react';
import { getEmpDeliveryList } from '../api/deliveryEmpApi'; // 💡 하단 설명 참조
import { getEmpOrderDetail, cancelEmpOrder } from '../api/orderEmpApi'; 
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

const TABS = [
  { key: null,        label: '전체 내역' },
  { key: 'READY',     label: '배송 준비중' },
  { key: 'SHIPPING',  label: '배송 중 🚚' },
  { key: 'COMPLETED', label: '배송 완료 ✅' },
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

// 💡 발주 상세 모달 (OrderEmpPage와 동일하게 재사용)
function OrderDetailModal({ orderId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getEmpOrderDetail(orderId)
      .then(setDetail)
      .catch(() => { Toast.error('상세 내역을 불러오지 못했습니다.'); onClose(); })
      .finally(() => setLoading(false));
  }, [orderId, onClose]);

  const formatDeliveryStatus = (status) => {
    if (status === 'READY') return '배송 준비중';
    if (status === 'SHIPPING') return '배송 중 🚚';
    if (status === 'COMPLETED') return '배송 완료 ✅';
    return status;
  };

  if (loading) return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40"><Spinner size="lg" /></div>;
  if (!detail) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4 shrink-0">
          <h2 className="text-lg font-bold text-slate-800">발주 및 배송 상세 내역</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>
        
        <div className="overflow-y-auto flex-1 pr-2 space-y-4">
          <div className="grid grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl text-sm">
            <div><span className="text-slate-500 block text-xs">주문 번호</span><span className="font-semibold">{detail.orderRequestId}</span></div>
            <div><span className="text-slate-500 block text-xs">신청 일시</span><span className="font-semibold">{new Date(detail.requestDate).toLocaleString('ko-KR')}</span></div>
            <div className="col-span-2 border-t border-slate-200 my-1"></div>
            <div>
              <span className="text-slate-500 block text-xs">배송 상태</span>
              <span className="font-bold text-emerald-600">{formatDeliveryStatus(detail.deliveryStatus)}</span>
            </div>
            <div>
              <span className="text-slate-500 block text-xs">운송장 번호</span>
              {detail.trackingNumber && detail.trackingNumber !== '등록된 운송장이 없습니다.' ? (
                <span className="font-mono font-semibold text-slate-700 bg-slate-200 px-2 py-0.5 rounded">
                  {detail.trackingNumber}
                </span>
              ) : (
                <span className="font-medium text-slate-400">{detail.trackingNumber}</span>
              )}
            </div>
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800 mb-2">신청 품목 ({detail.items?.length}건)</h3>
            <table className="min-w-full divide-y divide-slate-200 border border-slate-200 rounded-lg overflow-hidden">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-2 text-left text-[11px] font-semibold text-slate-500">상품명</th>
                  <th className="px-4 py-2 text-right text-[11px] font-semibold text-slate-500">수량</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {detail.items?.map((item, idx) => (
                  <tr key={idx}>
                    <td className="px-4 py-2 font-medium text-slate-800">{item.itemName}</td>
                    <td className="px-4 py-2 text-right text-slate-800 font-bold">{item.quantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="mt-6 pt-4 border-t border-slate-100 flex gap-2 shrink-0">
          <button onClick={onClose} className="w-full py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200">닫기</button>
        </div>
      </div>
    </div>
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

export default function DeliveryEmpPage() {
  const [deliveries, setDeliveries]     = useState([]);
  const [loading, setLoading]           = useState(true);
  const [page, setPage]                 = useState(0);
  const [totalPages, setTotalPages]     = useState(0);
  const [activeTab, setActiveTab]       = useState('SHIPPING'); // 💡 기본값을 '배송 중'으로 설정하여 목적 부합
  const [selectedOrderId, setSelectedOrderId] = useState(null);

  const load = useCallback((pg, status) => {
    setLoading(true);
    getEmpDeliveryList({ status: status || undefined, page: pg, size: PAGE_SIZE })
      .then((data) => {
        setDeliveries(data.content || []);
        setTotalPages(data.totalPages || 0);
      })
      .catch(() => { Toast.error('배송 목록을 불러오지 못했습니다.'); setDeliveries([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page, activeTab); }, [page, activeTab, load]);

  const handleTabChange = (key) => { setActiveTab(key); setPage(0); };
  const handleRefresh   = () => { load(page, activeTab); };

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      {/* ── 헤더 ── */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">내 배송 현황</h2>
          <p className="text-sm text-slate-500 mt-0.5">본인이 주문한 물품의 배송 진행 상황과 운송장을 확인합니다.</p>
        </div>
        <button onClick={handleRefresh} className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
          🔄 새로고침
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
                isActive ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
              }`}>
              {meta && <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-white/70' : meta.dot}`} />}
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── 콘텐츠 ── */}
      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      ) : deliveries.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message={activeTab ? `'${STATUS_META[activeTab]?.label}' 상태의 택배가 없습니다.` : '조회된 배송 내역이 없습니다.'} />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {['주문번호', '대표 상품명', '배송지', '배송 상태', '운송장 번호', '발송일'].map(label => (
                    <th key={label} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {deliveries.map((del) => (
                  <tr key={del.deliveryId} onClick={() => setSelectedOrderId(del.orderId)} className="hover:bg-slate-50 cursor-pointer transition-colors group">
                    <td className="px-5 py-3.5"><span className="text-sm font-bold text-slate-500 font-mono">#{del.orderId}</span></td>
                    <td className="px-5 py-3.5"><span className="text-sm font-semibold text-slate-800 group-hover:text-indigo-600">{del.representativeItemName || '발주 상품'}</span></td>
                    <td className="px-5 py-3.5">
                      <span className="text-sm text-slate-600 block truncate max-w-[200px]">{del.destinationAddress}</span>
                    </td>
                    <td className="px-5 py-3.5"><DeliveryStatusBadge status={del.status} /></td>
                    <td className="px-5 py-3.5">
                      {del.trackingNumber ? (
                        <span className="text-sm font-mono text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg border border-slate-200">
                          {del.trackingNumber}
                        </span>
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

      {/* ── 페이지네이션 & 모달 ── */}
      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}
      {selectedOrderId && <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} />}
    </div>
  );
}