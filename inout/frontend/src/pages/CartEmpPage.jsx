import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import client, { unwrap } from '../api/apiClient';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import AiSmartOrderPanel from '../components/dashboard/AiSmartOrderPanel';

export default function CartEmpPage() {
  const [cartData, setCartData]     = useState({ items: [], totalQuantity: 0, totalPrice: 0 });
  const [loading, setLoading]       = useState(true);
  const [updatingIds, setUpdatingIds] = useState(new Set()); 
  const navigate = useNavigate();

  const loadCart = () => {
    setLoading(true);
    unwrap(client.get('/emp/carts'))
      .then((data) => {
        setCartData({
          items: data?.items || [],
          totalQuantity: data?.totalQuantity || 0,
          totalPrice: data?.totalPrice || 0,
        });
      })
      .catch(() => Toast.error('장바구니를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadCart(); }, []);


  const handleQuantityChange = async (item, delta) => {
    const newQty = item.quantity + delta;
    if (newQty < 1) return;                    
    if (updatingIds.has(item.cartId)) return;  

    setCartData((prev) => {
      const updatedItems = prev.items.map((i) =>
        i.cartId === item.cartId
          ? { ...i, quantity: newQty, subTotal: i.unitPrice * newQty }
          : i
      );
      return {
        ...prev,
        items: updatedItems,
        totalQuantity: updatedItems.reduce((s, i) => s + i.quantity, 0),
        totalPrice:    updatedItems.reduce((s, i) => s + i.subTotal,  0),
      };
    });

    setUpdatingIds((prev) => new Set([...prev, item.cartId]));

    try {
      await unwrap(
        client.patch(`/emp/carts/${item.cartId}/quantity`, { quantity: newQty })
      );
    } catch {
      loadCart();
    } finally {
      setUpdatingIds((prev) => {
        const next = new Set(prev);
        next.delete(item.cartId);
        return next;
      });
    }
  };


  const handleDelete = async (ids) => {
    if (!window.confirm('선택한 상품을 삭제하시겠습니까?')) return;
    try {
      await unwrap(client.delete('/emp/carts/items', { data: ids }));
      Toast.success('삭제되었습니다.');
      loadCart();
    } catch {}
  };

const handleOrder = async () => {
  const ids = cartData.items.map((item) => item.cartId);
  try {

    const orderId = await unwrap(client.post('/emp/orders', { cartDetailIds: ids }));

    Toast.success('발주 기안이 등록되었습니다. 점주 승인 후 본사로 전달됩니다.');
    navigate('/emp/orders', { replace: true, state: { highlightOrderId: orderId } });
  } catch (err) {

  }
};

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800">장바구니</h2>
        <p className="text-sm text-slate-500 mt-0.5">발주 신청을 위해 담아둔 상품 목록입니다.</p>
      </div>

      <AiSmartOrderPanel onAdded={loadCart} />

      {loading ? (
        <div className="py-20 flex justify-center"><Spinner size="lg" /></div>
      ) : cartData.items.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 py-20 text-center text-slate-400">
          장바구니가 비어있습니다.
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">

          {/* ── 상품 목록 ── */}
          <div className="p-6 space-y-5">
            {cartData.items.map((item) => {
              const isUpdating = updatingIds.has(item.cartId);
              return (
                <div key={item.cartId}
                  className="flex items-start justify-between border-b border-slate-100 pb-5 last:border-0 last:pb-0 gap-4">

                  {/* 상품 정보 + 수량 컨트롤 */}
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-800 text-base truncate">{item.itemName}</p>
                    <p className="text-xs text-slate-400 mt-0.5">{item.unitPrice?.toLocaleString()}원 / 개</p>

                    {/* [−] 수량 [+] 컨트롤 */}
                    <div className="flex items-center gap-3 mt-2.5">
                      <div className={`inline-flex items-center rounded-xl border transition-colors ${
                        isUpdating ? 'border-indigo-300 bg-indigo-50' : 'border-slate-200 bg-white'
                      }`}>
                        <button onClick={() => handleQuantityChange(item, -1)} disabled={item.quantity <= 1 || isUpdating}
                          className="w-8 h-8 flex items-center justify-center text-slate-500 hover:bg-slate-100 rounded-l-xl disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 12h-15" /></svg>
                        </button>
                        <span className={`w-10 text-center text-sm font-bold select-none transition-colors ${isUpdating ? 'text-indigo-500' : 'text-slate-800'}`}>
                          {item.quantity}
                        </span>
                        <button onClick={() => handleQuantityChange(item, +1)} disabled={isUpdating}
                          className="w-8 h-8 flex items-center justify-center text-slate-500 hover:bg-slate-100 rounded-r-xl disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
                        </button>
                      </div>
                      {isUpdating && <span className="text-[11px] text-indigo-400 font-medium animate-pulse">저장 중...</span>}
                    </div>
                  </div>

                  {/* 소계 + 삭제 버튼 */}
                  <div className="text-right flex flex-col items-end gap-2 shrink-0">
                    <p className={`font-extrabold text-lg transition-colors ${isUpdating ? 'text-indigo-500' : 'text-slate-800'}`}>
                      {item.subTotal?.toLocaleString()}원
                    </p>
                    <button onClick={() => handleDelete([item.cartId])}
                      className="text-xs font-semibold text-rose-500 hover:text-rose-700 bg-rose-50 hover:bg-rose-100 px-2.5 py-1 rounded-lg transition-colors">
                      삭제
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* ── 합계 + 발주 버튼 ── */}
          <div className="bg-slate-50 border-t border-slate-200 p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500 font-medium">
                총 주문 수량: <span className="text-slate-800 font-bold">{cartData.totalQuantity}개</span>
              </p>
              <p className="text-sm text-slate-500 font-medium mt-1">총 결제 예상 금액</p>
              <p className="text-2xl font-black text-indigo-600 leading-none mt-1">
                {cartData.totalPrice?.toLocaleString()}원
              </p>
            </div>
            <button
              onClick={handleOrder}
              className="w-full sm:w-auto px-8 py-3.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 shadow-md shadow-indigo-200 transition-all active:scale-[0.98]">
              전체 발주 결제하기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}