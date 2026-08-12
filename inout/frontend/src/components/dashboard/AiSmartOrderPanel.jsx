import { useEffect, useState } from 'react';
import { getAiStockSuggestions } from '../../api/stockEmpApi';
import { addToCart } from '../../api/cartEmpApi';
import { Toast } from '../../utils/toast';
import Spinner from '../common/Spinner';

/**
 * 직원 장바구니용 스마트 발주 추천 패널
 * (최근 7일 판매 속도 + 안전재고 휴리스틱 — LLM 미사용)
 */
export default function AiSmartOrderPanel({ onAdded }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addingId, setAddingId] = useState(null);
  const [addingAll, setAddingAll] = useState(false);

  const load = () => {
    setLoading(true);
    getAiStockSuggestions(8)
      .then((data) => setItems(Array.isArray(data) ? data : []))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const addOne = async (item) => {
    if (!item?.itemId || !item?.recommendQty) return;
    setAddingId(item.itemId);
    try {
      await addToCart(item.itemId, item.recommendQty);
      Toast.success(`${item.itemName} ${item.recommendQty}개를 담았습니다.`);
      onAdded?.();
    } catch {
      /* interceptor */
    } finally {
      setAddingId(null);
    }
  };

  const addAll = async () => {
    if (items.length === 0) return;
    setAddingAll(true);
    let ok = 0;
    try {
      for (const item of items) {
        await addToCart(item.itemId, item.recommendQty);
        ok += 1;
      }
      Toast.success(`스마트 추천 ${ok}개 품목을 장바구니에 담았습니다.`);
      onAdded?.();
    } catch {
      if (ok > 0) {
        Toast.info(`${ok}개까지 담은 뒤 오류가 발생했습니다.`);
        onAdded?.();
      }
    } finally {
      setAddingAll(false);
    }
  };

  return (
    <section className="bg-gradient-to-br from-indigo-50 via-white to-teal-50 rounded-2xl border border-indigo-100 shadow-sm overflow-hidden">
      <div className="px-5 py-4 border-b border-indigo-100/80 flex items-center justify-between gap-3 flex-wrap">
        <div>
          <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-indigo-600 text-white text-xs font-bold">
              추천
            </span>
            스마트 추천
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            최근 7일 소진 속도와 안전재고를 분석한 추천 수량입니다.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={load}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50"
          >
            새로고침
          </button>
          <button
            type="button"
            onClick={addAll}
            disabled={addingAll || items.length === 0}
            className="px-3 py-1.5 text-xs font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {addingAll ? '담는 중…' : '추천 전부 담기'}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="py-10 flex justify-center"><Spinner /></div>
      ) : items.length === 0 ? (
        <p className="text-sm text-center text-slate-400 py-10">현재 발주가 시급한 추천 품목이 없습니다.</p>
      ) : (
        <ul className="divide-y divide-indigo-50">
          {items.map((item) => (
            <li key={item.itemId} className="px-5 py-3.5 flex items-start justify-between gap-4">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <p className="text-sm font-bold text-slate-800 truncate">{item.itemName}</p>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-teal-100 text-teal-700 border border-teal-200">
                    추천 {item.recommendQty}개
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  현재 {item.currentStock?.toLocaleString('ko-KR')} · 안전 {item.minStockLevel?.toLocaleString('ko-KR')}
                  {item.unitPrice != null && ` · ${Number(item.unitPrice).toLocaleString('ko-KR')}원`}
                </p>
                {item.reason && (
                  <p className="text-[11px] text-indigo-600/80 mt-1 leading-snug">{item.reason}</p>
                )}
              </div>
              <button
                type="button"
                onClick={() => addOne(item)}
                disabled={addingId === item.itemId || addingAll}
                className="shrink-0 px-3 py-1.5 text-xs font-bold rounded-lg bg-white border border-indigo-200 text-indigo-700 hover:bg-indigo-50 disabled:opacity-50"
              >
                {addingId === item.itemId ? '담는 중…' : '추천 수량 담기'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
