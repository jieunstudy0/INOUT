import AuthImage from '../components/common/AuthImage';
import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getEmpStockDetail } from '../api/stockEmpApi';
import { addToCart } from '../api/cartEmpApi';
import { Toast } from '../utils/toast';
import { DEFAULT_ITEM_IMAGE } from '../utils/images';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { stockBadge } from '../utils/stockStatus';

const TONE_CLS = { teal: 'bg-emerald-100 text-emerald-600', orange: 'bg-amber-100 text-amber-600', red: 'bg-rose-50 text-rose-600' };

/** 직원 — 본사 상품 상세 (/emp/stocks/:itemId). 본사 상품 정보는 본사만 고칠 수 있어 여기서는 보고 담기만 한다. */
export default function StockEmpDetailPage() {
  const { itemId } = useParams();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    getEmpStockDetail(itemId)
      .then(setDetail)
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, [itemId]);

  const add = async () => {
    if (quantity < 1 || quantity > detail.currentStock) {
      Toast.warning(`주문 가능 수량은 1 ~ ${detail.currentStock}개입니다.`);
      return;
    }
    setAdding(true);
    try {
      await addToCart(detail.itemId, quantity);
      Toast.success('장바구니에 담았습니다.');
    } catch { /* interceptor */ } finally { setAdding(false); }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner size="lg" /></div>;
  if (!detail) return <EmptyState message="상품을 찾을 수 없습니다." />;
  const badge = stockBadge(detail.currentStock ?? 0, 0);

  return (
    <div className="space-y-6 max-w-5xl">
      <Link to="/emp/stocks" className="text-sm text-slate-500 hover:text-slate-900">‹ 본사 상품</Link>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
        <div className="aspect-[4/3] rounded-[10px] border border-slate-200 bg-slate-50 overflow-hidden">
          <AuthImage src={detail.imageUrl} fallback={DEFAULT_ITEM_IMAGE} alt={detail.name} className="w-full h-full object-cover" />
        </div>
        <div>
          <p className="text-sm text-slate-500">{detail.categoryName}</p>
          <h2 className="mt-1 text-3xl font-normal text-slate-900">{detail.name}</h2>
          <p className="mt-4 text-2xl font-semibold text-slate-900">{(detail.unitPrice ?? 0).toLocaleString('ko-KR')}원
            <span className="text-sm font-normal text-slate-500 ml-1">/ {detail.unitDescription || '개'}</span></p>
          <p className="mt-3 flex items-center gap-2 text-sm text-slate-600">
            <span className={`px-1.5 py-0.5 rounded-[10px] text-xs ${TONE_CLS[badge.tone]}`}>{badge.label}</span>
            본사 재고 {(detail.currentStock ?? 0).toLocaleString()}{detail.unitDescription || '개'}
          </p>
          {detail.description && <p className="mt-5 text-slate-700 whitespace-pre-wrap leading-relaxed">{detail.description}</p>}

          <div className="mt-8 rounded-[10px] bg-slate-50 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-700">주문 수량</span>
              <input type="number" min="1" max={detail.currentStock} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}
                className="w-24 px-3 py-2 text-right text-sm border border-slate-300 rounded-[10px] bg-white" />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-700">합계</span>
              <span className="text-xl font-semibold text-slate-900">{((detail.unitPrice ?? 0) * quantity).toLocaleString('ko-KR')}원</span>
            </div>
            <button onClick={add} disabled={adding || detail.currentStock === 0}
              className="w-full py-3 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
              {adding ? '담는 중...' : detail.currentStock === 0 ? '품절' : '장바구니 담기'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
