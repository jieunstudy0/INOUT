import { useState, useEffect, useCallback } from 'react';
import { getEmpStockList, getEmpStockDetail } from '../api/stockEmpApi';
import { addToCart } from '../api/cartEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';


function ItemDetailModal({ itemId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    getEmpStockDetail(itemId)
      .then(setDetail)
      .catch(() => { Toast.error('상품 정보를 불러오지 못했습니다.'); onClose(); })
      .finally(() => setLoading(false));
  }, [itemId, onClose]);

  const handleAddToCart = async () => {
    if (quantity < 1 || quantity > detail.currentStock) {
      return Toast.warning(`주문 가능 수량은 1 ~ ${detail.currentStock}개 입니다.`);
    }
    setAdding(true);
    try {
      await addToCart(detail.itemId, quantity);
      Toast.success('장바구니에 추가되었습니다.');
      onClose();
    } catch (err) {
 
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40"><Spinner size="lg" /></div>;
  if (!detail) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <span className="text-xs font-semibold text-indigo-500 bg-indigo-50 px-2 py-1 rounded-md">{detail.categoryName}</span>
            <h2 className="text-xl font-bold text-slate-800 mt-2">{detail.name}</h2>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg></button>
        </div>
        
        <div className="bg-slate-50 p-4 rounded-xl space-y-3 mb-6">
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">단가</span>
            <span className="font-semibold text-slate-800">{detail.unitPrice?.toLocaleString()}원</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">본사 재고</span>
            <span className={`font-bold ${detail.currentStock === 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
              {detail.currentStock?.toLocaleString()}개
            </span>
          </div>
          <div className="flex justify-between text-sm items-center border-t border-slate-200 pt-3">
            <span className="text-slate-500 font-medium">주문 수량</span>
            <input type="number" min="1" max={detail.currentStock} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}
              className="w-20 px-2 py-1 text-right border border-slate-300 rounded focus:outline-none focus:border-indigo-500" />
          </div>
          <div className="flex justify-between text-base items-center border-t border-slate-200 pt-3">
            <span className="text-slate-800 font-bold">총 주문 금액</span>
            <span className="font-extrabold text-indigo-600">{(detail.unitPrice * quantity).toLocaleString()}원</span>
          </div>
        </div>

        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-3 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200">취소</button>
          <button onClick={handleAddToCart} disabled={adding || detail.currentStock === 0} 
            className="flex-1 py-3 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed">
            {adding ? <Spinner size="sm" /> : '장바구니 담기'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function StockEmpPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchName, setSearchName] = useState('');
  const [inputName, setInputName] = useState('');
  const [selectedItemId, setSelectedItemId] = useState(null);

  const loadItems = useCallback((name) => {
    setLoading(true);
    getEmpStockList({ name: name || undefined, page: 0, size: 50 }) 
      .then((data) => setItems(data?.content || []))
      .catch(() => { Toast.error('상품 목록을 불러오지 못했습니다.'); setItems([]); })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadItems(searchName); }, [searchName, loadItems]);

  const handleQuickAdd = async (e, item) => {
    e.stopPropagation(); 
    if (item.currentStock < 1) return Toast.warning('재고가 품절되었습니다.');
    try {
      await addToCart(item.itemId, 1);
      Toast.success(`[${item.name}] 장바구니에 1개 담겼습니다.`);
    } catch (err) {}
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">본사 재고 조회 (발주 신청)</h2>
        <p className="text-sm text-slate-500 mt-0.5">본사의 현재 재고를 확인하고 발주를 위해 장바구니에 담을 수 있습니다.</p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm px-5 py-4">
        <form onSubmit={(e) => { e.preventDefault(); setSearchName(inputName.trim()); }} className="flex gap-3">
          <input type="text" value={inputName} onChange={(e) => setInputName(e.target.value)} placeholder="상품명 검색..."
            className="flex-1 px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500" />
          <button type="submit" className="px-6 py-2.5 text-sm font-semibold bg-slate-800 text-white rounded-xl hover:bg-slate-900">검색</button>
        </form>
      </div>

      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) 
      : items.length === 0 ? ( <EmptyState message="상품이 없습니다." /> ) 
      : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                {['상품명', '카테고리', '단가', '본사 재고', '장바구니'].map(h => <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase">{h}</th>)}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((item) => (
                <tr key={item.itemId} onClick={() => setSelectedItemId(item.itemId)} className="hover:bg-slate-50 cursor-pointer transition-colors group">
                  <td className="px-5 py-4 text-sm font-semibold text-slate-800 group-hover:text-indigo-600">{item.name}</td>
                  <td className="px-5 py-4 text-sm text-slate-500">{item.categoryName}</td>
                  <td className="px-5 py-4 text-sm text-slate-700">{item.unitPrice?.toLocaleString()}원</td>
                  <td className="px-5 py-4">
                    <span className={`text-sm font-bold ${item.currentStock === 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
                      {item.currentStock === 0 ? '품절' : `${item.currentStock?.toLocaleString()}개`}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <button onClick={(e) => handleQuickAdd(e, item)} disabled={item.currentStock === 0}
                      className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-100 hover:bg-indigo-100 disabled:opacity-40 transition-all">
                      + 담기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedItemId && <ItemDetailModal itemId={selectedItemId} onClose={() => setSelectedItemId(null)} />}
    </div>
  );
}