import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

import { payWithDeposit } from '../api/paymentApi';
import { getMyDepositHistory } from '../api/depositEmpApi';
import client, { unwrap } from '../api/apiClient';
import { dispatchHeaderRefresh } from '../utils/headerSync';

export default function PaymentEmpPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [balance, setBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [orderData, depositData] = await Promise.all([
          unwrap(client.get(`/emp/orders/${orderId}`)),
          getMyDepositHistory(0, 1)
        ]);
        
        setOrder(orderData);
        setBalance(depositData?.currentBalance ?? 0);
      } catch (err) {
        Toast.error('결제 정보를 불러오지 못했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [orderId, navigate]);

  const handlePayment = async () => {
    if (balance < order.totalPrice) {
      return Toast.warning('예치금 잔액이 부족합니다. 점주에게 충전 신청을 요청해 주세요.');
    }

    if (!window.confirm('정말 예치금으로 결제하시겠습니까?')) return;

    setPaying(true);
    try {
      const response = await payWithDeposit(order.orderRequestId, order.totalPrice);
      
      Toast.success(
        <div>
          <p>{response.message || '결제가 성공적으로 처리되었습니다.'}</p>
          <p className="text-xs mt-1 opacity-80">남은 예치금: {response.remainingBalance?.toLocaleString()}원</p>
        </div>
      );
      dispatchHeaderRefresh({ role: 'EMPLOYEE' });
      navigate('/emp/orders', { replace: true });
    } catch (err) {
    } finally {
      setPaying(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;
  if (!order) return null;

  const isShortage = balance < order.totalPrice;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">결제하기</h2>
        <p className="text-sm text-slate-500 mt-1">발주 내역을 확인하고 예치금으로 대금을 결제합니다.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-800 mb-4 pb-4 border-b border-slate-100">주문 요약</h3>
            <div className="space-y-4">
              <div className="flex justify-between items-center text-sm">
                <span className="text-slate-500">주문 번호</span>
                <span className="font-semibold text-slate-800">#{order.orderRequestId}</span>
              </div>
              <div className="flex justify-between items-center text-sm">
                <span className="text-slate-500">주문 일시</span>
                <span className="text-slate-800">
                  {new Date(order.requestDate).toLocaleString('ko-KR')}
                </span>
              </div>
              <div className="flex justify-between items-center text-sm">
                <span className="text-slate-500">배송지</span>
                <span className="text-slate-800 text-right">
                  {order.receiverName} ({order.receiverPhone})<br />
                  <span className="text-xs text-slate-500">{order.destinationAddress}</span>
                </span>
              </div>
            </div>
            
            <div className="mt-6 pt-4 border-t border-slate-100">
              <p className="text-xs font-semibold text-slate-400 mb-3">주문 상품</p>
              <ul className="space-y-3">
                {order.items?.map((item, idx) => (
                  <li key={idx} className="flex justify-between items-center text-sm">
                    <span className="text-slate-700">{item.itemName} <span className="text-slate-400 text-xs ml-1">x {item.quantity}</span></span>
                    <span className="font-semibold text-slate-800">{item.subTotal?.toLocaleString()}원</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sticky top-24">
            <h3 className="text-base font-bold text-slate-800 mb-4 pb-4 border-b border-slate-100">결제 정보</h3>
            
            <div className="space-y-4 mb-6">
              <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500">총 상품 금액</span>
                <span className="text-sm font-semibold text-slate-800">{order.totalPrice?.toLocaleString()}원</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-slate-500">현재 예치금</span>
                <span className="text-sm font-semibold text-emerald-600">{balance.toLocaleString()}원</span>
              </div>
              
              <div className="pt-4 border-t border-slate-200/60 flex justify-between items-center">
                <span className="text-base font-bold text-slate-800">최종 결제 금액</span>
                <span className="text-xl font-extrabold text-indigo-600">
                  {order.totalPrice?.toLocaleString()}원
                </span>
              </div>

              <div className="flex justify-between items-center bg-slate-50 p-3 rounded-lg mt-2">
                <span className="text-xs font-semibold text-slate-500">결제 후 예상 잔액</span>
                <span className={`text-sm font-bold ${isShortage ? 'text-rose-500' : 'text-slate-700'}`}>
                  {(balance - order.totalPrice).toLocaleString()}원
                </span>
              </div>
              {isShortage && (
                <p className="text-xs text-rose-500 text-center font-medium">예치금이 부족합니다.</p>
              )}
            </div>

            <button 
              onClick={handlePayment} 
              disabled={paying || isShortage}
              className="w-full py-3.5 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 disabled:opacity-40 transition-all flex items-center justify-center gap-2"
            >
              {paying ? <Spinner size="sm" /> : '결제하기'}
            </button>
            <button 
              onClick={() => navigate(-1)} 
              disabled={paying}
              className="w-full py-3.5 mt-2 bg-white border border-slate-300 text-slate-600 font-bold rounded-xl hover:bg-slate-50 transition-all"
            >
              취소
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}