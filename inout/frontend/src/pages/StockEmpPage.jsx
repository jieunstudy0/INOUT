import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEmpStockList } from '../api/stockEmpApi';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import ProductCard from '../components/common/ProductCard';
import { stockBadge, stockTone } from '../utils/stockStatus';

/** 직원 — 본사 상품 조회. 상품을 누르면 상세 화면(사진·설명·재고·장바구니 담기)으로 간다. */
export default function StockEmpPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchName, setSearchName] = useState('');
  const [inputName, setInputName] = useState('');

  const loadItems = useCallback((name) => {
    setLoading(true);
    getEmpStockList({ name: name || undefined, page: 0, size: 60 })
      .then((data) => setItems(data?.content || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadItems(searchName); }, [searchName, loadItems]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-normal text-slate-900">본사 상품</h2>
        <p className="mt-2 text-slate-600">발주할 수 있는 본사 상품입니다. 상품을 누르면 자세한 정보와 장바구니 담기가 있습니다.</p>
      </div>

      <form onSubmit={(e) => { e.preventDefault(); setSearchName(inputName.trim()); }} className="flex gap-2">
        <input value={inputName} onChange={(e) => setInputName(e.target.value)} placeholder="상품명 검색"
          className="flex-1 px-4 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30" />
        <button type="submit" className="px-6 py-2.5 text-sm rounded-full bg-indigo-600 text-white hover:bg-indigo-700">검색</button>
      </form>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : items.length === 0 ? (
        <EmptyState message="검색된 상품이 없습니다." />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-x-6 gap-y-9">
          {items.map((item) => (
            <ProductCard key={item.itemId}
              imageUrl={item.imageUrl}
              name={item.name}
              badges={[stockBadge(item.currentStock ?? 0, 0)]}
              stockLabel="본사 재고"
              stock={{ value: item.currentStock ?? 0, unit: item.unitDescription, tone: stockTone(item.currentStock ?? 0, 0) }}
              sub={`${item.categoryName} · ${(item.unitPrice ?? 0).toLocaleString('ko-KR')}원`}
              description={item.description}
              onClick={() => navigate(`/emp/stocks/${item.itemId}`)} />
          ))}
        </div>
      )}
    </div>
  );
}
