import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; // 💡 useNavigate 추가
import { getMyOrderHistory, getEmpOrderDetail, cancelEmpOrder } from '../api/orderEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';


function OrderStatusBadge({ status }) {
  const config = {
    REQUESTED: { label: '결제대기(승인대기)', cls: 'bg-slate-100 text-slate-600' },
    PAID:      { label: '결제완료', cls: 'bg-blue-100 text-blue-700' },
    APPROVED:  { label: '승인됨',   cls: 'bg-indigo-100 text-indigo-700' },
    PARTIAL:   { label: '부분승인', cls: 'bg-amber-100 text-amber-700' },
    COMPLETED: { label: '처리완료', cls: 'bg-emerald-100 text-emerald-700' },
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
    const isPaid = detail.status === 'PAID';
    const confirmMessage = isPaid
      ? `결제 완료된 발주입니다.\n취소 시 예치금 ${detail.totalPrice?.toLocaleString()}원이 즉시 환불됩니다.\n\n정말 취소하시겠습니까?`
      : '정말 이 발주를 취소하시겠습니까?';

    if (!window.confirm(confirmMessage)) return;
    try {
      await cancelEmpOrder(orderId);
      Toast.success(isPaid ? '발주가 취소되었습니다. 예치금이 환불되었습니다.' : '발주가 취소되었습니다.');
      onRefresh();
      onClose();
    } catch (err) {}
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
          {(detail.status === 'REQUESTED' || detail.status === 'PAID') && (
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
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  
  const navigate = useNavigate(); // 💡 useNavigate 선언

  const fetchOrders = () => {
    setLoading(true);
    getMyOrderHistory()
      .then(setOrders)
      .catch(() => Toast.error('발주 이력을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchOrders(); }, []);

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">내 발주 내역</h2>
        <p className="text-sm text-slate-500 mt-0.5">내가 신청한 발주 내역과 진행 상태를 확인합니다.</p>
      </div>

      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) 
      : orders.length === 0 ? ( <EmptyState message="신청한 발주 내역이 없습니다." /> ) 
      : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>

                {['주문 번호', '신청 일시', '대표 상품명', '총 금액', '상태', '결제'].map(h => (
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
                    {order.status === 'REQUESTED' && (
                      <button 
                        onClick={(e) => {
                          e.stopPropagation(); // 💡 부모 tr의 onClick(상세 모달 열기) 실행 방지
                          navigate(`/emp/payment/${order.orderRequestId}`); // 결제 페이지로 이동
                        }}
                        className="px-4 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-lg hover:bg-indigo-700 transition-colors shadow-sm"
                      >
                        결제하기
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedOrderId && <OrderDetailModal orderId={selectedOrderId} onClose={() => setSelectedOrderId(null)} onRefresh={fetchOrders} />}
    </div>
  );
}