import { useState, useEffect, useCallback, useMemo } from 'react';
import { getStoreStocks, consumeStoreStock } from '../api/storeStockApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import ChangeRequestForm from '../components/request/ChangeRequestForm';
import ProductCard from '../components/common/ProductCard';
import { stockBadge, stockTone } from '../utils/stockStatus';
import { confirmDialog } from '../utils/dialog';

/**
 * 매장 재고 사용 처리 (직원).
 * 소속 매장 재고에서만 차감한다. 본사 재고는 발주 승인 때만 바뀐다.
 * 사진 카드로 상품을 바로 알아보고, 카드 아래에서 쓴 만큼 기록한다.
 */
export default function StockUseEmpPage() {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [onlyLow, setOnlyLow] = useState(false);
  const [inputs, setInputs] = useState({});
  const [submittingId, setSubmittingId] = useState(null);
  const [requestFor, setRequestFor] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getStoreStocks('emp')
      .then((data) => setStocks(data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const visible = useMemo(() => {
    const k = keyword.trim();
    return stocks.filter((s) => (!onlyLow || s.low) && (!k || s.itemName.includes(k) || (s.location || '').includes(k.toUpperCase())));
  }, [stocks, keyword, onlyLow]);

  const handleUse = async (stock) => {
    const qty = Number(inputs[stock.storeStockId]);
    if (!qty || qty < 1) return Toast.warning('사용할 수량을 1개 이상 입력해 주세요.');
    if (qty > stock.quantity) return Toast.warning('매장 재고보다 많이 사용할 수 없습니다.');
    if (!await confirmDialog(`[${stock.itemName}] ${qty}개를 사용 처리할까요?`)) return;

    setSubmittingId(stock.storeStockId);
    try {
      await consumeStoreStock(stock.itemId, qty, '매장 사용');
      Toast.success('사용 처리했습니다.');
      setInputs((prev) => ({ ...prev, [stock.storeStockId]: '' }));
      load();
    } catch {
      /* interceptor */
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-normal text-slate-900">매장 재고</h2>
        <p className="mt-2 text-slate-600">우리 매장 재고입니다. 사용한 만큼 바로 기록해 주세요.</p>
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <input type="search" value={keyword} onChange={(e) => setKeyword(e.target.value)}
          placeholder="상품명 또는 위치(C-01) 검색"
          className="flex-1 px-4 py-2.5 text-sm bg-white border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30" />
        <label className="flex items-center gap-2 px-3 py-2.5 text-sm bg-white border border-slate-300 rounded-[10px]">
          <input type="checkbox" checked={onlyLow} onChange={(e) => setOnlyLow(e.target.checked)} />
          부족한 것만
        </label>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : stocks.length === 0 ? (
        <EmptyState message="아직 매장 재고가 없습니다. 배송을 수령 확인하거나 점주가 처음 재고를 등록하면 나타납니다." />
      ) : visible.length === 0 ? (
        <EmptyState message="조건에 맞는 상품이 없습니다." />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-x-5 gap-y-9">
          {visible.map((s) => {
            const value = inputs[s.storeStockId] || '';
            const over = Number(value) > s.quantity;
            return (
              <ProductCard key={s.storeStockId}
                imageUrl={s.imageUrl}
                name={s.itemName}
                badges={[stockBadge(s.quantity, s.minStockLevel)]}
                stock={{ value: s.quantity, unit: s.unitDescription, tone: stockTone(s.quantity, s.minStockLevel) }}
                sub={`${s.location ? `위치 ${s.location} · ` : ''}안전재고 ${s.minStockLevel}`}>
                {s.quantity > 0 ? (
                  <div className="flex items-center gap-2">
                    <input type="number" inputMode="numeric" min="1" max={s.quantity} value={value} placeholder="사용 수량"
                      onChange={(e) => setInputs((prev) => ({ ...prev, [s.storeStockId]: e.target.value }))}
                      className={`flex-1 min-w-0 px-3 py-2 text-right text-sm border rounded-[10px] focus:outline-none focus:ring-2 ${over ? 'border-rose-400 focus:ring-rose-500' : 'border-slate-300 focus:ring-indigo-500/30'}`} />
                    <button onClick={() => handleUse(s)} disabled={submittingId === s.storeStockId || over || !value}
                      className="px-4 py-2 text-sm rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40">
                      {submittingId === s.storeStockId ? '처리 중' : '사용'}
                    </button>
                  </div>
                ) : (
                  <p className="px-3 py-2 text-center text-xs text-rose-600 bg-rose-50 rounded-[10px]">재고 없음 — 발주가 필요합니다</p>
                )}
                <button onClick={() => setRequestFor(s)} className="mt-2 text-xs text-slate-500 underline underline-offset-2 hover:text-slate-900">
                  수량이 다르면 정정 요청
                </button>
              </ProductCard>
            );
          })}
        </div>
      )}

      {requestFor && (
        <ChangeRequestForm role="emp" onClose={() => setRequestFor(null)}
          preset={{
            category: 'STOCK_CORRECTION',
            refType: 'STORE_STOCK',
            refId: requestFor.storeStockId,
            refLabel: `${requestFor.itemName} (기록 ${requestFor.quantity})`,
            title: `${requestFor.itemName} 수량 정정`,
            content: `기록은 ${requestFor.quantity}개인데 실제로는 __개입니다.\n이유: `,
          }} />
      )}
    </div>
  );
}
