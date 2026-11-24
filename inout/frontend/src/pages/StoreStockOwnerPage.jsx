import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getStoreStocks } from '../api/storeStockApi';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import ProductCard from '../components/common/ProductCard';
import { stockBadge, stockTone } from '../utils/stockStatus';

/** 점주 매장 재고 목록. 상품을 누르면 상세 화면에서 조정·설정·이력을 다룬다. */
export default function StoreStockOwnerPage() {
  const navigate = useNavigate();
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [onlyLow, setOnlyLow] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getStoreStocks('owner')
      .then((data) => setStocks(data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const visible = useMemo(() => {
    const k = keyword.trim();
    return stocks.filter((s) => (!onlyLow || s.low) && (!k || s.itemName.includes(k) || (s.location || '').includes(k.toUpperCase())));
  }, [stocks, keyword, onlyLow]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-3xl font-normal text-slate-900">매장 재고 관리</h2>
          <p className="mt-2 text-slate-600">직원은 사용(차감)만 할 수 있고, 수량 조정·처음 등록·안전재고·위치는 점주가 관리합니다.</p>
        </div>
        <button onClick={() => navigate('/owner/store-stocks/new')}
          className="px-5 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700">
          새 상품 등록
        </button>
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <input type="search" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="상품명 또는 위치 검색"
          className="flex-1 px-4 py-2.5 text-sm bg-white border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30" />
        <label className="flex items-center gap-2 px-3 py-2.5 text-sm bg-white border border-slate-300 rounded-[10px]">
          <input type="checkbox" checked={onlyLow} onChange={(e) => setOnlyLow(e.target.checked)} />
          부족한 것만
        </label>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : visible.length === 0 ? (
        <EmptyState message={stocks.length === 0 ? '매장 재고가 없습니다. [새 상품 등록]으로 처음 재고를 넣거나, 배송을 수령 확인하면 생깁니다.' : '조건에 맞는 상품이 없습니다.'} />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-x-6 gap-y-9">
          {visible.map((s) => (
            <ProductCard key={s.storeStockId}
              imageUrl={s.imageUrl}
              name={s.itemName}
              badges={[stockBadge(s.quantity, s.minStockLevel)]}
              stock={{ value: s.quantity, unit: s.unitDescription, tone: stockTone(s.quantity, s.minStockLevel) }}
              sub={`위치 ${s.location || '미지정'} · 안전재고 ${s.minStockLevel}`}
              onClick={() => navigate(`/owner/store-stocks/${s.storeStockId}`)} />
          ))}
        </div>
      )}
    </div>
  );
}
