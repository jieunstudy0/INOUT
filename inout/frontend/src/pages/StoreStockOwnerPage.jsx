import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  getStoreStocks,
  getStoreCatalog,
  adjustStoreStock,
  updateStoreStockSettings,
  getStoreStockHistory,
} from '../api/storeStockApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { formatDateTime } from '../utils/stockCount';

const inputCls = 'w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40';

function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="absolute inset-0 bg-slate-900/40" onClick={onClose} />
      <div className="relative bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-2xl shadow-2xl max-h-[85vh] overflow-y-auto">
        <div className="flex items-center justify-between px-5 py-3 border-b border-slate-100">
          <h3 className="text-sm font-bold text-slate-800">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 px-2">닫기</button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}

/** 점주 매장 재고 관리: 조정(사유 필수)·새 상품 등록·안전재고와 위치 설정·이력 */
export default function StoreStockOwnerPage() {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [modal, setModal] = useState(null); // { type: 'adjust'|'settings'|'history'|'register', stock }
  const [form, setForm] = useState({});
  const [catalog, setCatalog] = useState([]);
  const [history, setHistory] = useState([]);
  const [saving, setSaving] = useState(false);

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
    return stocks.filter((s) => !k || s.itemName.includes(k) || (s.location || '').includes(k.toUpperCase()));
  }, [stocks, keyword]);

  const openModal = (type, stock) => {
    setModal({ type, stock });
    if (type === 'adjust') setForm({ actualQuantity: String(stock.quantity), reason: '' });
    if (type === 'settings') setForm({ minStockLevel: String(stock.minStockLevel), location: stock.location || '' });
    if (type === 'register') {
      setForm({ itemId: '', actualQuantity: '', reason: '처음 재고 등록' });
      getStoreCatalog().then((d) => setCatalog((d || []).filter((c) => !c.registered))).catch(() => {});
    }
    if (type === 'history') {
      setHistory([]);
      getStoreStockHistory('owner', stock.storeStockId).then((d) => setHistory(d || [])).catch(() => {});
    }
  };

  const close = () => setModal(null);

  const save = async () => {
    setSaving(true);
    try {
      if (modal.type === 'adjust' || modal.type === 'register') {
        const itemId = modal.type === 'register' ? Number(form.itemId) : modal.stock.itemId;
        if (!itemId) { Toast.warning('상품을 선택해 주세요.'); return; }
        if (form.actualQuantity === '' || Number(form.actualQuantity) < 0) { Toast.warning('실제 수량을 0 이상으로 입력해 주세요.'); return; }
        if (!form.reason.trim()) { Toast.warning('사유를 입력해 주세요.'); return; }
        await adjustStoreStock(itemId, Number(form.actualQuantity), form.reason.trim());
        Toast.success(modal.type === 'register' ? '매장 재고에 등록했습니다.' : '재고를 조정했습니다.');
      } else if (modal.type === 'settings') {
        await updateStoreStockSettings(modal.stock.storeStockId, Number(form.minStockLevel || 0), form.location.trim());
        Toast.success('설정을 바꿨습니다.');
      }
      close();
      load();
    } catch {
      /* interceptor */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4 max-w-6xl mx-auto">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">매장 재고 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            직원은 사용(차감)만 할 수 있고, 수량 조정·처음 등록·안전재고·위치는 점주가 관리합니다. 본사 재고와는 별개입니다.
          </p>
        </div>
        <button onClick={() => openModal('register')}
          className="px-4 py-2.5 text-sm font-semibold bg-slate-800 text-white rounded-xl hover:bg-slate-900">
          새 상품 등록
        </button>
      </div>

      <input type="search" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="상품명 또는 위치 검색"
        className="w-full px-4 py-2.5 text-sm bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500" />

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : visible.length === 0 ? (
        <EmptyState message="매장 재고가 없습니다. [새 상품 등록]으로 처음 재고를 넣거나, 배송을 수령 확인하면 생깁니다." />
      ) : (
        <ul className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          {visible.map((s) => (
            <li key={s.storeStockId} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-bold text-slate-800 truncate">{s.itemName}</div>
                  <div className="text-xs text-slate-400 mt-0.5">위치 {s.location || '미지정'} · 안전재고 {s.minStockLevel}</div>
                </div>
                <div className={`text-lg font-bold shrink-0 ${s.quantity === 0 ? 'text-rose-600' : s.low ? 'text-amber-600' : 'text-slate-800'}`}>
                  {s.quantity}<span className="text-xs font-medium text-slate-400 ml-0.5">{s.unitDescription || '개'}</span>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-2 mt-3">
                <button onClick={() => openModal('adjust', s)} className="py-2 text-xs font-semibold rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100">조정</button>
                <button onClick={() => openModal('settings', s)} className="py-2 text-xs font-semibold rounded-lg bg-slate-50 text-slate-700 hover:bg-slate-100">설정</button>
                <button onClick={() => openModal('history', s)} className="py-2 text-xs font-semibold rounded-lg bg-slate-50 text-slate-700 hover:bg-slate-100">이력</button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {modal?.type === 'register' && (
        <Modal title="매장 재고에 새 상품 등록" onClose={close}>
          <div className="space-y-3">
            <select value={form.itemId} onChange={(e) => setForm({ ...form, itemId: e.target.value })} className={inputCls}>
              <option value="">본사 상품 선택</option>
              {catalog.map((c) => <option key={c.itemId} value={c.itemId}>{c.itemName} ({c.categoryName})</option>)}
            </select>
            <input type="number" min="0" inputMode="numeric" placeholder="지금 매장에 있는 수량" value={form.actualQuantity}
              onChange={(e) => setForm({ ...form, actualQuantity: e.target.value })} className={inputCls} />
            <input type="text" maxLength={100} placeholder="사유" value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })} className={inputCls} />
            <button onClick={save} disabled={saving} className="w-full py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white disabled:opacity-50">등록</button>
          </div>
        </Modal>
      )}

      {modal?.type === 'adjust' && (
        <Modal title={`재고 조정 — ${modal.stock.itemName}`} onClose={close}>
          <div className="space-y-3">
            <p className="text-xs text-slate-500">지금 {modal.stock.quantity}개로 기록되어 있습니다. 실제 수량을 입력하면 차이만큼 조정되고 이력이 남습니다.</p>
            <input type="number" min="0" inputMode="numeric" value={form.actualQuantity}
              onChange={(e) => setForm({ ...form, actualQuantity: e.target.value })} className={inputCls} />
            <input type="text" maxLength={100} placeholder="사유 (예: 파손 2개 폐기, 매장 자체 구매)" value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })} className={inputCls} />
            <button onClick={save} disabled={saving} className="w-full py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white disabled:opacity-50">조정</button>
          </div>
        </Modal>
      )}

      {modal?.type === 'settings' && (
        <Modal title={`설정 — ${modal.stock.itemName}`} onClose={close}>
          <div className="space-y-3">
            <label className="block text-xs font-semibold text-slate-600">안전재고 (이 수량 이하이면 실사표·부족 목록에 오릅니다)</label>
            <input type="number" min="0" inputMode="numeric" value={form.minStockLevel}
              onChange={(e) => setForm({ ...form, minStockLevel: e.target.value })} className={inputCls} />
            <label className="block text-xs font-semibold text-slate-600">매장 보관 위치 (실사 영수증 정렬 순서)</label>
            <input type="text" maxLength={20} placeholder="예: C-01" value={form.location}
              onChange={(e) => setForm({ ...form, location: e.target.value })} className={inputCls} />
            <button onClick={save} disabled={saving} className="w-full py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white disabled:opacity-50">저장</button>
          </div>
        </Modal>
      )}

      {modal?.type === 'history' && (
        <Modal title={`최근 이력 — ${modal.stock.itemName}`} onClose={close}>
          {history.length === 0 ? (
            <p className="text-sm text-slate-400">이력이 없습니다.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {history.map((h) => (
                <li key={h.historyId} className="py-2 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="text-sm text-slate-800"><b>{h.type}</b> · {h.userName}</div>
                    <div className="text-xs text-slate-400 truncate">{h.memo || '-'} · {formatDateTime(h.createdAt)}</div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className={`text-sm font-bold ${h.changeQty > 0 ? 'text-emerald-600' : 'text-rose-600'}`}>{h.changeQty > 0 ? `+${h.changeQty}` : h.changeQty}</div>
                    <div className="text-[11px] text-slate-400">→ {h.resultQty}</div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Modal>
      )}
    </div>
  );
}
