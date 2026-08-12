import { useState, useEffect, useCallback } from 'react';
import { getOwnerOrders, getOwnerOrderDetail } from '../api/orderOwnerApi';
import { getOwnerDeliveryList } from '../api/deliveryOwnerApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import DeliveryTrackingModal from '../components/delivery/DeliveryTrackingModal';
import OrderModifyModal from '../components/order/OrderModifyModal';
import PersonName from '../components/common/PersonName';

const PAGE_SIZE = 10;

const ORDER_TABS = [
  { key: null, label: '전체' },
  { key: 'REQUESTED', label: '기안 승인 대기' },
  { key: 'ORDERED', label: '본사 승인 대기' },
  { key: 'APPROVED', label: '본사 승인 완료' },
  { key: 'REJECTED', label: '반려' },
  { key: 'CANCELLED', label: '취소' },
];

const DELIVERY_TABS = [
  { key: null, label: '전체' },
  { key: 'READY', label: '배송 준비' },
  { key: 'SHIPPING', label: '배송 중' },
  { key: 'COMPLETED', label: '배송 완료' },
];

const ORDER_STATUS = {
  REQUESTED: { label: '기안 승인 대기', cls: 'bg-amber-100 text-amber-800' },
  ORDERED: { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
  APPROVED: { label: '본사 승인 완료', cls: 'bg-emerald-100 text-emerald-700' },
  PAID: { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
  PARTIAL: { label: '부분 처리', cls: 'bg-amber-100 text-amber-700' },
  COMPLETED: { label: '본사 승인 완료', cls: 'bg-emerald-100 text-emerald-700' },
  REJECTED: { label: '반려', cls: 'bg-rose-100 text-rose-700' },
  CANCELLED: { label: '취소', cls: 'bg-gray-100 text-gray-500' },
};

const DELIVERY_STATUS = {
  READY: { label: '배송 준비', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  SHIPPING: { label: '배송 중', cls: 'bg-blue-50 text-blue-700 border-blue-200' },
  COMPLETED: { label: '배송 완료', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
};

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
        <button key={p} onClick={() => onPageChange(p)} className={`${base} ${p === page ? 'bg-emerald-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${base} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

function OrderDetailModal({ orderId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getOwnerOrderDetail(orderId)
      .then(setDetail)
      .catch(() => { Toast.error('발주 상세를 불러오지 못했습니다.'); onClose(); })
      .finally(() => setLoading(false));
  }, [orderId, onClose]);

  if (loading) {
    return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40"><Spinner size="lg" /></div>;
  }
  if (!detail) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold text-slate-800">발주 상세 #{detail.orderRequestId}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>
        <div className="overflow-y-auto space-y-4 text-sm">
          <div className="grid grid-cols-2 gap-3 bg-slate-50 p-4 rounded-xl">
            <div><span className="text-xs text-slate-500 block">신청자</span><span className="font-semibold"><PersonName name={detail.employeeName} /></span></div>
            <div><span className="text-xs text-slate-500 block">상태</span><span className="font-semibold">{ORDER_STATUS[detail.status]?.label || detail.status}</span></div>
            <div><span className="text-xs text-slate-500 block">신청일시</span><span className="font-semibold">{detail.requestDate ? new Date(detail.requestDate).toLocaleString('ko-KR') : '-'}</span></div>
            <div><span className="text-xs text-slate-500 block">총액</span><span className="font-bold text-emerald-700">{Number(detail.totalPrice || 0).toLocaleString('ko-KR')}원</span></div>
          </div>
          {detail.rejectReason && (
            <div className="bg-rose-50 border border-rose-100 rounded-xl px-4 py-3 text-sm text-rose-700">
              반려 사유: {detail.rejectReason}
            </div>
          )}
          <table className="min-w-full border border-slate-200 rounded-lg overflow-hidden">
            <thead className="bg-slate-50 text-xs text-slate-500">
              <tr>
                <th className="px-3 py-2 text-left">품목</th>
                <th className="px-3 py-2 text-right">단가</th>
                <th className="px-3 py-2 text-right">수량</th>
                <th className="px-3 py-2 text-right">소계</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(detail.items || []).map((item) => (
                <tr key={item.orderDetailId}>
                  <td className="px-3 py-2 font-medium text-slate-800">{item.itemName}</td>
                  <td className="px-3 py-2 text-right">{Number(item.priceSnapshot || 0).toLocaleString('ko-KR')}</td>
                  <td className="px-3 py-2 text-right font-bold">{item.quantity}</td>
                  <td className="px-3 py-2 text-right font-bold">{Number(item.subTotal || 0).toLocaleString('ko-KR')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <button onClick={onClose} className="mt-4 w-full py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl">닫기</button>
      </div>
    </div>
  );
}

export default function OwnerOrderList() {
  const [view, setView] = useState('orders');
  const [orders, setOrders] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [orderStatus, setOrderStatus] = useState('REQUESTED');
  const [deliveryStatus, setDeliveryStatus] = useState(null);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [modifyOrderId, setModifyOrderId] = useState(null);
  const [trackingTarget, setTrackingTarget] = useState(null);

  const loadOrders = useCallback((pg, status) => {
    setLoading(true);
    getOwnerOrders({ page: pg, size: PAGE_SIZE, status: status || undefined })
      .then((data) => {
        setOrders(data?.content || []);
        setTotalPages(data?.totalPages || 0);
      })
      .catch(() => Toast.error('발주 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  const loadDeliveries = useCallback((pg, status) => {
    setLoading(true);
    getOwnerDeliveryList({ page: pg, size: PAGE_SIZE, status: status || undefined })
      .then((data) => {
        setDeliveries(data?.content || []);
        setTotalPages(data?.totalPages || 0);
      })
      .catch(() => Toast.error('배송 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setPage(0);
  }, [view, orderStatus, deliveryStatus]);

  useEffect(() => {
    if (view === 'orders') loadOrders(page, orderStatus);
    else loadDeliveries(page, deliveryStatus);
  }, [view, page, orderStatus, deliveryStatus, loadOrders, loadDeliveries]);

  const tabs = view === 'orders' ? ORDER_TABS : DELIVERY_TABS;
  const activeTab = view === 'orders' ? orderStatus : deliveryStatus;
  const setActiveTab = view === 'orders' ? setOrderStatus : setDeliveryStatus;

  const refreshOrders = () => loadOrders(page, orderStatus);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">매장 발주/배송</h2>
        <p className="text-sm text-slate-500 mt-0.5">직원 기안 검토·결제 후 본사 승인까지 3단계로 관리합니다.</p>
      </div>

      <div className="flex gap-2">
        <button
          onClick={() => setView('orders')}
          className={`px-4 py-2 text-sm font-bold rounded-xl border transition-colors ${view === 'orders' ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-white text-slate-600 border-slate-200'}`}
        >
          발주 내역
        </button>
        <button
          onClick={() => setView('deliveries')}
          className={`px-4 py-2 text-sm font-bold rounded-xl border transition-colors ${view === 'deliveries' ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-white text-slate-600 border-slate-200'}`}
        >
          배송 현황
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <button
            key={String(tab.key)}
            onClick={() => setActiveTab(tab.key)}
            className={`px-3 py-1.5 text-xs font-bold rounded-full border ${activeTab === tab.key ? 'bg-slate-800 text-white border-slate-800' : 'bg-white text-slate-600 border-slate-200'}`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <section className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-10 flex justify-center"><Spinner /></div>
        ) : view === 'orders' ? (
          orders.length === 0 ? (
            <div className="p-6"><EmptyState title="발주 내역이 없습니다." /></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5 text-left font-semibold">주문</th>
                    <th className="px-4 py-2.5 text-left font-semibold">신청자</th>
                    <th className="px-4 py-2.5 text-left font-semibold">상태</th>
                    <th className="px-4 py-2.5 text-right font-semibold">금액</th>
                    <th className="px-4 py-2.5 text-right font-semibold">처리</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {orders.map((o) => (
                    <tr key={o.orderRequestId} className="hover:bg-slate-50/80">
                      <td className="px-4 py-3">
                        <p className="font-semibold text-slate-800">#{o.orderRequestId}</p>
                        <p className="text-xs text-slate-400">{o.representativeItemName}{o.itemCount > 1 ? ` 외 ${o.itemCount - 1}건` : ''}</p>
                        <p className="text-[11px] text-slate-400 mt-0.5">{o.requestDate ? new Date(o.requestDate).toLocaleString('ko-KR') : '-'}</p>
                      </td>
                      <td className="px-4 py-3 text-slate-600"><PersonName name={o.employeeName} /></td>
                      <td className="px-4 py-3">
                        <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${ORDER_STATUS[o.status]?.cls || 'bg-slate-100 text-slate-600'}`}>
                          {ORDER_STATUS[o.status]?.label || o.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-bold tabular-nums">{Number(o.totalPrice || 0).toLocaleString('ko-KR')}원</td>
                      <td className="px-4 py-3 text-right">
                        {o.status === 'REQUESTED' ? (
                          <button
                            type="button"
                            onClick={() => setModifyOrderId(o.orderRequestId)}
                            className="text-xs font-semibold text-emerald-700 hover:underline"
                          >
                            검토·결제
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setSelectedOrderId(o.orderRequestId)}
                            className="text-xs font-semibold text-slate-600 hover:underline"
                          >
                            보기
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        ) : deliveries.length === 0 ? (
          <div className="p-6"><EmptyState title="배송 내역이 없습니다." /></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 text-xs text-slate-500">
                <tr>
                  <th className="px-4 py-2.5 text-left font-semibold">배송/주문</th>
                  <th className="px-4 py-2.5 text-left font-semibold">수령인</th>
                  <th className="px-4 py-2.5 text-left font-semibold">상태</th>
                  <th className="px-4 py-2.5 text-left font-semibold">운송장</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {deliveries.map((d) => (
                  <tr key={d.deliveryId}>
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-800">배송 #{d.deliveryId}</p>
                      <p className="text-xs text-slate-400">주문 #{d.orderId}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-600">{d.receiverName || '-'}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold border ${DELIVERY_STATUS[d.status]?.cls || 'bg-slate-100 text-slate-600'}`}>
                        {DELIVERY_STATUS[d.status]?.label || d.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {d.trackingNumber ? (
                        <div className="flex items-center gap-2 flex-wrap">
                          <div className="flex flex-col gap-0.5">
                            {d.carrier && <span className="text-[10px] text-slate-400">{d.carrier}</span>}
                            <span className="font-mono text-xs text-slate-700">{d.trackingNumber}</span>
                          </div>
                          <button
                            type="button"
                            onClick={() => setTrackingTarget(d)}
                            className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-semibold rounded-lg bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-100"
                          >
                            배송 조회
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-400">-</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="px-5 pb-5">
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </div>
      </section>

      {selectedOrderId && (
        <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} />
      )}

      {modifyOrderId && (
        <OrderModifyModal
          orderId={modifyOrderId}
          onClose={() => setModifyOrderId(null)}
          onDone={refreshOrders}
        />
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
