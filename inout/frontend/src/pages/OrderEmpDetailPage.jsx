import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEmpOrderDetail, cancelEmpOrder } from '../api/orderEmpApi.js';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { useAppBasePath } from '../utils/appPaths';

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

// 영문 배송 상태가 넘어올 경우를 대비한 한글 변환기
function formatDeliveryStatus(status) {
  if (status === 'READY') return '배송 준비중';
  if (status === 'SHIPPING') return '배송 중 🚚';
  if (status === 'COMPLETED') return '배송 완료 ✅';
  return status || '배송 대기'; 
}

export default function OrderEmpDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const base = useAppBasePath();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadDetail = useCallback(() => {
    setLoading(true);
    getEmpOrderDetail(orderId)
      .then(setDetail)
      .catch(() => { 
        Toast.error('상세 내역을 불러오지 못했습니다.'); 
        navigate(`${base}/orders`);
      })
      .finally(() => setLoading(false));
  }, [orderId, navigate, base]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  const handleCancel = async () => {
    const isPaid = detail.status === 'PAID';
    const confirmMessage = isPaid
      ? `결제 완료된 발주입니다.\n취소 시 예치금 ${detail.totalPrice?.toLocaleString()}원이 즉시 환불됩니다.\n\n정말 취소하시겠습니까?`
      : '정말 이 발주를 취소하시겠습니까?';

    if (!window.confirm(confirmMessage)) return;
    try {
      await cancelEmpOrder(orderId);
      Toast.success(isPaid ? '발주가 취소되었습니다. 예치금이 환불되었습니다.' : '발주가 취소되었습니다.');
      loadDetail(); // 페이지 데이터 갱신
    } catch (err) {}
  };

  if (loading) return <div className="flex justify-center py-32"><Spinner size="lg" /></div>;
  if (!detail) return <EmptyState message="발주 정보를 찾을 수 없습니다." />;

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate(`${base}/orders`)} className="text-slate-400 hover:text-indigo-600 p-2 bg-white rounded-full shadow-sm border border-slate-200">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div>
          <h2 className="text-xl font-bold text-slate-800">발주 상세 내역</h2>
          <p className="text-sm text-slate-500 mt-0.5">#{detail.orderRequestId} 발주의 처리 상태 및 배송 정보를 확인합니다.</p>
        </div>
        <div className="ml-auto flex gap-2">
            {(detail.status === 'REQUESTED' || detail.status === 'PAID') && (
                <button onClick={handleCancel} className="px-4 py-2 text-sm font-bold bg-rose-600 text-white rounded-xl hover:bg-rose-700">
                  발주 취소
                </button>
            )}
            {detail.status === 'REQUESTED' && (
                <button 
                  onClick={() => navigate(`${base}/payment/${detail.orderRequestId}`)}
                  className="px-4 py-2 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700"
                >
                  결제하기
                </button>
            )}
        </div>
      </div>

      {/* 정보 섹션 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 발주 정보 */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-800 border-b border-slate-100 pb-2">주문 정보</h3>
            <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                <div><span className="text-slate-500 block text-xs">신청 일시</span><span className="font-semibold text-slate-700">{new Date(detail.requestDate).toLocaleString('ko-KR')}</span></div>
                <div><span className="text-slate-500 block text-xs">진행 상태</span><OrderStatusBadge status={detail.status} /></div>
                <div className="col-span-2"><span className="text-slate-500 block text-xs">총 주문 금액</span><span className="text-xl font-bold text-indigo-600">{detail.totalPrice?.toLocaleString()}원</span></div>
            </div>
            {detail.rejectReason && (
              <div className="p-3 bg-rose-50 rounded-lg border border-rose-100 text-xs text-rose-700">
                <span className="font-semibold">반려 사유: </span>{detail.rejectReason}
              </div>
            )}
        </div>

        {/* 배송 정보 */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-800 border-b border-slate-100 pb-2">배송 정보</h3>
            <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                <div>
                  <span className="text-slate-500 block text-xs">배송 상태</span>
                  <span className="font-bold text-emerald-600">{formatDeliveryStatus(detail.deliveryStatus)}</span>
                </div>
                <div>
                  <span className="text-slate-500 block text-xs">운송장 번호</span>
                  {detail.trackingNumber && detail.trackingNumber !== '등록된 운송장이 없습니다.' ? (
                    <span className="font-mono font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                      {detail.trackingNumber}
                    </span>
                  ) : (
                    <span className="font-medium text-slate-400">{detail.trackingNumber || '미등록'}</span>
                  )}
                </div>
                <div className="col-span-2"><span className="text-slate-500 block text-xs">수신처 (주소)</span><span className="font-medium text-slate-700">{detail.deliveryAddress || '-'}</span></div>
                <div><span className="text-slate-500 block text-xs">수신자</span><span className="font-medium text-slate-700">{detail.recipientName || '-'}</span></div>
                <div><span className="text-slate-500 block text-xs">연락처</span><span className="font-medium text-slate-700">{detail.recipientPhone || '-'}</span></div>
            </div>
        </div>
      </div>

      {/* 품목 테이블 */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200">
          <h3 className="text-sm font-bold text-slate-800">신청 품목 ({detail.items?.length}건)</h3>
        </div>
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase">상품명</th>
              <th className="px-6 py-3 text-right text-[11px] font-semibold text-slate-500 uppercase">발주 단가</th>
              <th className="px-6 py-3 text-right text-[11px] font-semibold text-slate-500 uppercase">수량</th>
              <th className="px-6 py-3 text-right text-[11px] font-semibold text-slate-500 uppercase">소계</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm bg-white">
            {detail.items?.map((item, idx) => (
              <tr key={idx} className="hover:bg-slate-50">
                <td className="px-6 py-4 font-medium text-slate-800">{item.itemName}</td>
                <td className="px-6 py-4 text-right text-slate-600">{item.priceSnapshot?.toLocaleString()}원</td>
                <td className="px-6 py-4 text-right text-slate-800 font-bold">{item.quantity}</td>
                <td className="px-6 py-4 text-right text-slate-800 font-bold">{item.subTotal?.toLocaleString()}원</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}