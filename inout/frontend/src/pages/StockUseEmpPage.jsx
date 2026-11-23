import { useState, useEffect, useCallback, useMemo } from 'react';
import { getStoreStocks, consumeStoreStock } from '../api/storeStockApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import ChangeRequestForm from '../components/request/ChangeRequestForm';

/**
 * 매장 재고 사용 처리 (직원).
 * 소속 매장 재고에서만 차감한다. 본사 재고는 발주 승인 때만 바뀐다.
 * 현장에서 휴대폰으로 쓰는 화면이라 표 대신 카드 목록으로 보여 준다.
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
    if (!window.confirm(`[${stock.itemName}] ${qty}개를 사용 처리할까요?`)) return;

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
    <div className="space-y-4 max-w-5xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">매장 재고 · 사용 처리</h2>
        <p className="text-sm text-slate-500 mt-0.5">우리 매장 재고입니다. 사용한 만큼 바로 기록해 주세요.</p>
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <input type="search" value={keyword} onChange={(e) => setKeyword(e.target.value)}
          placeholder="상품명 또는 위치(C-01) 검색"
          className="flex-1 px-4 py-2.5 text-sm bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500" />
        <label className="flex items-center gap-2 px-3 py-2.5 text-sm bg-white border border-slate-200 rounded-xl">
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
        <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {visible.map((s) => {
            const value = inputs[s.storeStockId] || '';
            const over = Number(value) > s.quantity;
            return (
              <li key={s.storeStockId} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="text-sm font-bold text-slate-800 truncate">{s.itemName}</div>
                    <div className="text-xs text-slate-400 mt-0.5">
                      {s.location ? `위치 ${s.location} · ` : ''}{s.categoryName}
                      <button onClick={() => setRequestFor(s)} className="ml-2 text-indigo-600 underline underline-offset-2">수량이 다르면 정정 요청</button>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className={`text-lg font-bold ${s.quantity === 0 ? 'text-rose-600' : s.low ? 'text-amber-600' : 'text-slate-800'}`}>
                      {s.quantity.toLocaleString()}<span className="text-xs font-medium text-slate-400 ml-0.5">{s.unitDescription || '개'}</span>
                    </div>
                    <div className="text-[11px] text-slate-400">안전재고 {s.minStockLevel}</div>
                  </div>
                </div>
                {s.quantity > 0 ? (
                  <div className="flex items-center gap-2 mt-3">
                    <input type="number" inputMode="numeric" min="1" max={s.quantity} value={value} placeholder="사용 수량"
                      onChange={(e) => setInputs((prev) => ({ ...prev, [s.storeStockId]: e.target.value }))}
                      className={`flex-1 min-w-0 px-3 py-2.5 text-right text-sm border rounded-xl focus:outline-none focus:ring-2 ${over ? 'border-rose-400 focus:ring-rose-500' : 'border-slate-300 focus:ring-indigo-500'}`} />
                    <button onClick={() => handleUse(s)} disabled={submittingId === s.storeStockId || over || !value}
                      className="px-5 py-2.5 text-sm font-bold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40">
                      {submittingId === s.storeStockId ? '처리 중' : '사용'}
                    </button>
                  </div>
                ) : (
                  <div className="mt-3 px-3 py-2 text-center text-xs font-semibold text-rose-600 bg-rose-50 rounded-xl">재고 없음 — 발주가 필요합니다</div>
                )}
              </li>
            );
          })}
        </ul>
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
