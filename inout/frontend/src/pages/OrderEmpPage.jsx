import { useState, useEffect, useCallback } from 'react';
import { getMyOrderHistory, getEmpOrderDetail, cancelEmpOrder } from '../api/orderEmpApi';
import { getEmpDeliveryList } from '../api/deliveryEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import DeliveryTrackingModal from '../components/delivery/DeliveryTrackingModal';

const DELIVERY_STATUS = {
  READY: { label: '배송 준비', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  SHIPPING: { label: '배송 중', cls: 'bg-blue-50 text-blue-700 border-blue-200' },
  COMPLETED: { label: '배송 완료', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
};

function OrderStatusBadge({ status }) {
  const config = {
    REQUESTED: { label: '점주 승인 대기', cls: 'bg-amber-100 text-amber-800' },
    ORDERED:   { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
    PAID:      { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
    APPROVED:  { label: '본사 승인 완료', cls: 'bg-emerald-100 text-emerald-700' },
    PARTIAL:   { label: '부분승인', cls: 'bg-amber-100 text-amber-700' },
    COMPLETED: { label: '본사 승인 완료', cls: 'bg-emerald-100 text-emerald-700' },
    REJECTED:  { label: '반려됨',   cls: 'bg-rose-100 text-rose-700' },
    CANCELLED: { label: '발주취소', cls: 'bg-gray-100 text-gray-500 line-through' },
  }[status] || { label: status, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${config.cls}`}>{config.label}</span>;
}

function OrderDetailModal({ orderId, onClose, onRefresh }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getEmpOrderDetail(orderId)
      .then(setDetail)
      .catch(() => { Toast.error('상세 내역을 불러오지 못했습니다.'); onClose(); })
      .finally(() => setLoading(false));
  }, [orderId, onClose]);

  const handleCancel = async () => {
    const isPaid = detail.status === 'ORDERED' || detail.status === 'PAID';
    const confirmMessage = isPaid
      ? `결제 완료된 발주입니다.\n취소 시 예치금 ${detail.totalPrice?.toLocaleString()}원이 즉시 환불됩니다.\n\n정말 취소하시겠습니까?`
      : '정말 이 발주를 취소하시겠습니까?';

    if (!window.confirm(confirmMessage)) return;
    try {
      await cancelEmpOrder(orderId);
      Toast.success(isPaid ? '발주가 취소되었습니다. 예치금이 환불되었습니다.' : '발주가 취소되었습니다.');
      onRefresh();
      onClose();
    } catch (err) { /* */ }
  };

  if (loading) return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40"><Spinner size="lg" /></div>;
  if (!detail) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4 shrink-0">
          <h2 className="text-lg font-bold text-slate-800">발주 상세 내역</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>

        <div className="overflow-y-auto flex-1 pr-2 space-y-4">
          <div className="grid grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl text-sm">
            <div><span className="text-slate-500 block text-xs">주문 번호</span><span className="font-semibold">{detail.orderRequestId}</span></div>
            <div><span className="text-slate-500 block text-xs">신청 일시</span><span className="font-semibold">{new Date(detail.requestDate).toLocaleString('ko-KR')}</span></div>
            <div><span className="text-slate-500 block text-xs">진행 상태</span><OrderStatusBadge status={detail.status} /></div>
            <div><span className="text-slate-500 block text-xs">총 주문 금액</span><span className="font-bold text-indigo-600">{detail.totalPrice?.toLocaleString()}원</span></div>
          </div>

          <div>
            <h3 className="text-sm font-bold text-slate-800 mb-2">신청 품목 ({detail.items?.length}건)</h3>
            <table className="min-w-full divide-y divide-slate-200 border border-slate-200 rounded-lg overflow-hidden">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-2 text-left text-[11px] font-semibold text-slate-500">상품명</th>
                  <th className="px-4 py-2 text-right text-[11px] font-semibold text-slate-500">발주 단가</th>
                  <th className="px-4 py-2 text-right text-[11px] font-semibold text-slate-500">수량</th>
                  <th className="px-4 py-2 text-right text-[11px] font-semibold text-slate-500">소계</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {detail.items?.map((item, idx) => (
                  <tr key={idx}>
                    <td className="px-4 py-2 font-medium text-slate-800">{item.itemName}</td>
                    <td className="px-4 py-2 text-right text-slate-600">{item.priceSnapshot?.toLocaleString()}원</td>
                    <td className="px-4 py-2 text-right text-slate-800 font-bold">{item.quantity}</td>
                    <td className="px-4 py-2 text-right text-slate-800 font-bold">{item.subTotal?.toLocaleString()}원</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-slate-100 flex gap-2 shrink-0">
          <button onClick={onClose} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200">닫기</button>
          {(detail.status === 'REQUESTED' || detail.status === 'ORDERED' || detail.status === 'PAID') && (
            <button onClick={handleCancel} className="flex-1 py-2.5 text-sm font-bold bg-rose-600 text-white rounded-xl hover:bg-rose-700">
              발주 취소
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default function OrderEmpPage() {
  const [view, setView] = useState('orders');
  const [orders, setOrders] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [trackingTarget, setTrackingTarget] = useState(null);

  const fetchOrders = useCallback(() => {
    setLoading(true);
    getMyOrderHistory()
      .then(setOrders)
      .catch(() => Toast.error('발주 이력을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  const fetchDeliveries = useCallback(() => {
    setLoading(true);
    getEmpDeliveryList({ size: 50 })
      .then((page) => setDeliveries(page?.content || []))
      .catch(() => Toast.error('배송 내역을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (view === 'orders') fetchOrders();
    else fetchDeliveries();
  }, [view, fetchOrders, fetchDeliveries]);

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">내 발주 내역</h2>
        <p className="text-sm text-slate-500 mt-0.5">내가 신청한 발주와 배송 현황을 확인합니다.</p>
      </div>

      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setView('orders')}
          className={`px-4 py-2 text-sm font-bold rounded-xl border transition-colors ${view === 'orders' ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-600 border-slate-200'}`}
        >
          발주 내역
        </button>
        <button
          type="button"
          onClick={() => setView('deliveries')}
          className={`px-4 py-2 text-sm font-bold rounded-xl border transition-colors ${view === 'deliveries' ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-600 border-slate-200'}`}
        >
          배송 현황
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : view === 'orders' ? (
        orders.length === 0 ? (
          <EmptyState message="신청한 발주 내역이 없습니다." />
        ) : (
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  {['주문 번호', '신청 일시', '대표 상품명', '총 금액', '상태', '결제'].map((h) => (
                    <th key={h} className={`px-5 py-3 text-[11px] font-semibold text-slate-500 uppercase ${h === '결제' ? 'text-center' : 'text-left'}`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((order) => (
                  <tr key={order.orderRequestId} onClick={() => setSelectedOrderId(order.orderRequestId)} className="hover:bg-slate-50 cursor-pointer transition-colors">
                    <td className="px-5 py-4 text-sm text-slate-500">#{order.orderRequestId}</td>
                    <td className="px-5 py-4 text-sm text-slate-600">{new Date(order.requestDate).toLocaleDateString('ko-KR')}</td>
                    <td className="px-5 py-4 text-sm font-semibold text-slate-800">{order.representativeItemName}</td>
                    <td className="px-5 py-4 text-sm font-bold text-slate-800">{order.totalPrice?.toLocaleString()}원</td>
                    <td className="px-5 py-4"><OrderStatusBadge status={order.status} /></td>
                    <td className="px-5 py-4 text-center">
                      {order.status === 'REQUESTED' ? (
                        <span className="text-[11px] font-semibold text-amber-700">점주 승인 대기</span>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : deliveries.length === 0 ? (
        <EmptyState message="배송 내역이 없습니다." />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">배송/주문</th>
                <th className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">상태</th>
                <th className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">운송장</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {deliveries.map((d) => (
                <tr key={d.deliveryId}>
                  <td className="px-5 py-4">
                    <p className="font-semibold text-slate-800">배송 #{d.deliveryId}</p>
                    <p className="text-xs text-slate-400">주문 #{d.orderId}</p>
                  </td>
                  <td className="px-5 py-4">
                    <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold border ${DELIVERY_STATUS[d.status]?.cls || 'bg-slate-100 text-slate-600'}`}>
                      {DELIVERY_STATUS[d.status]?.label || d.status}
                    </span>
                  </td>
                  <td className="px-5 py-4">
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
                          🚚 배송 조회
                        </button>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-400">미발급</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedOrderId && (
        <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} onRefresh={fetchOrders} />
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
