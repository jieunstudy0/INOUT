import AuthImage from '../components/common/AuthImage';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  getOwnerStoreStock, getStoreStockHistory, adjustStoreStock, updateStoreStockSettings, getStoreCatalog,
} from '../api/storeStockApi';
import { Toast } from '../utils/toast';
import { DEFAULT_ITEM_IMAGE } from '../utils/images';
import { formatDateTime } from '../utils/stockCount';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { stockBadge } from '../utils/stockStatus';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30';
const TONE_CLS = { teal: 'bg-emerald-100 text-emerald-600', orange: 'bg-amber-100 text-amber-600', red: 'bg-rose-50 text-rose-600' };
const btnOutline = 'px-5 py-2.5 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50 disabled:opacity-50';

/** 사진 큰 칸 */
function Photo({ src, alt }) {
  return (
    <div className="aspect-[4/3] rounded-[10px] border border-slate-200 bg-slate-50 overflow-hidden">
      <AuthImage src={src} fallback={DEFAULT_ITEM_IMAGE} alt={alt} className="w-full h-full object-cover" />
    </div>
  );
}

/** 새 상품 등록: 본사 상품을 사진으로 고르고 지금 매장에 있는 수량을 넣는다. */
function RegisterView() {
  const navigate = useNavigate();
  const [catalog, setCatalog] = useState([]);
  const [loading, setLoading] = useState(true);
  const [picked, setPicked] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [form, setForm] = useState({ quantity: '', reason: '처음 재고 등록' });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getStoreCatalog().then((d) => setCatalog((d || []).filter((c) => !c.registered))).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const visible = useMemo(() => catalog.filter((c) => !keyword.trim() || c.itemName.includes(keyword.trim())), [catalog, keyword]);

  const save = async () => {
    if (!picked) { Toast.warning('등록할 상품을 골라 주세요.'); return; }
    if (form.quantity === '' || Number(form.quantity) < 0) { Toast.warning('지금 매장에 있는 수량을 0 이상으로 입력해 주세요.'); return; }
    if (!form.reason.trim()) { Toast.warning('사유를 입력해 주세요.'); return; }
    setSaving(true);
    try {
      await adjustStoreStock(picked.itemId, Number(form.quantity), form.reason.trim());
      Toast.success('매장 재고에 등록했습니다.');
      navigate('/owner/store-stocks');
    } catch { /* interceptor */ } finally { setSaving(false); }
  };

  return (
    <div className="space-y-6">
      <div>
        <Link to="/owner/store-stocks" className="text-sm text-slate-500 hover:text-slate-900">‹ 매장 재고 관리</Link>
        <h2 className="mt-2 text-3xl font-normal text-slate-900">새 상품 등록</h2>
        <p className="mt-2 text-slate-600">매장에서 다룰 본사 상품을 고르고, 지금 매장에 있는 수량을 넣어 주세요.</p>
      </div>
      <input type="search" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="상품명 검색" className={inputCls} />
      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : visible.length === 0 ? (
        <EmptyState message="등록할 수 있는 상품이 없습니다." />
      ) : (
        <div className="grid grid-cols-3 md:grid-cols-4 xl:grid-cols-6 gap-4">
          {visible.map((c) => (
            <button key={c.itemId} type="button" onClick={() => setPicked(c)}
              className={`text-left rounded-[10px] p-2 border-2 transition-colors ${picked?.itemId === c.itemId ? 'border-indigo-600 bg-indigo-50' : 'border-transparent hover:bg-slate-50'}`}>
              <Photo src={c.imageUrl} alt={c.itemName} />
              <p className="mt-2 text-sm text-slate-900 line-clamp-2">{c.itemName}</p>
              <p className="text-xs text-slate-500">{c.categoryName}</p>
            </button>
          ))}
        </div>
      )}
      <div className="sticky bottom-0 bg-white border-t border-slate-200 py-4 flex flex-wrap items-center gap-3">
        <span className="text-sm text-slate-700 min-w-40">{picked ? <b className="font-semibold text-slate-900">{picked.itemName}</b> : '상품을 고르세요'}</span>
        <input type="number" min="0" placeholder="지금 수량" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} className={`${inputCls} w-32`} />
        <input placeholder="사유" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} className={`${inputCls} w-56`} />
        <button type="button" onClick={save} disabled={saving} className="px-6 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
          {saving ? '등록 중...' : '등록'}
        </button>
      </div>
    </div>
  );
}

/** 매장 재고 상세: 사진·상품 정보, 안전재고·위치 저장, 수량 조정, 이력 */
function DetailView({ storeStockId }) {
  const [stock, setStock] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [settings, setSettings] = useState({ minStockLevel: '', location: '' });
  const [adjust, setAdjust] = useState({ actual: '', reason: '' });
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([getOwnerStoreStock(storeStockId), getStoreStockHistory('owner', storeStockId)])
      .then(([s, h]) => {
        setStock(s);
        setHistory(h || []);
        setSettings({ minStockLevel: String(s.minStockLevel), location: s.location || '' });
      })
      .catch(() => setStock(null))
      .finally(() => setLoading(false));
  }, [storeStockId]);

  useEffect(() => { load(); }, [load]);

  const saveSettings = async () => {
    if (settings.minStockLevel === '' || Number(settings.minStockLevel) < 0) { Toast.warning('안전재고를 0 이상으로 입력해 주세요.'); return; }
    setSaving(true);
    try {
      await updateStoreStockSettings(storeStockId, Number(settings.minStockLevel), settings.location.trim());
      Toast.success('저장했습니다.');
      load();
    } catch { /* interceptor */ } finally { setSaving(false); }
  };

  const doAdjust = async () => {
    if (adjust.actual === '' || Number(adjust.actual) < 0) { Toast.warning('실제 수량을 0 이상으로 입력해 주세요.'); return; }
    if (!adjust.reason.trim()) { Toast.warning('조정 사유를 입력해 주세요.'); return; }
    setSaving(true);
    try {
      await adjustStoreStock(stock.itemId, Number(adjust.actual), adjust.reason.trim());
      Toast.success('재고를 조정했습니다.');
      setAdjust({ actual: '', reason: '' });
      load();
    } catch { /* interceptor */ } finally { setSaving(false); }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner size="lg" /></div>;
  if (!stock) return <EmptyState message="매장 재고를 찾을 수 없습니다." />;
  const badge = stockBadge(stock.quantity, stock.minStockLevel);

  return (
    <div className="space-y-8 max-w-5xl">
      <div>
        <Link to="/owner/store-stocks" className="text-sm text-slate-500 hover:text-slate-900">‹ 매장 재고 관리</Link>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-[360px_1fr] gap-10">
        <Photo src={stock.imageUrl} alt={stock.itemName} />
        <div>
          <p className="text-sm text-slate-500">{stock.categoryName}</p>
          <h2 className="mt-1 text-3xl font-normal text-slate-900">{stock.itemName}</h2>
          <p className="mt-4 text-3xl font-semibold text-slate-900 tabular-nums">
            {stock.quantity.toLocaleString()}<span className="text-base font-normal text-slate-500 ml-1">{stock.unitDescription || '개'}</span>
          </p>
          <p className="mt-2 flex items-center gap-2 text-sm text-slate-600">
            <span className={`px-1.5 py-0.5 rounded-[10px] text-xs ${TONE_CLS[badge.tone]}`}>{badge.label}</span>
            우리 매장 재고 · 마지막 변경 {formatDateTime(stock.updatedAt)}
          </p>
          {stock.description && <p className="mt-4 text-slate-700 whitespace-pre-wrap">{stock.description}</p>}
          <p className="mt-3 text-xs text-slate-400">상품 사진·이름·설명은 본사 상품 정보입니다. 본사에서 바꿀 수 있습니다.</p>
        </div>
      </div>

      <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3">
          <h3 className="text-xl font-normal text-slate-900 pb-3 border-b border-slate-900">매장 설정</h3>
          <label className="block text-sm text-slate-700">안전재고 <span className="text-slate-400">(이 수량 이하이면 실사표·부족 목록에 오릅니다)</span>
            <input type="number" min="0" value={settings.minStockLevel} onChange={(e) => setSettings({ ...settings, minStockLevel: e.target.value })} className={`${inputCls} mt-1.5`} />
          </label>
          <label className="block text-sm text-slate-700">매장 보관 위치 <span className="text-slate-400">(실사 영수증 정렬 순서)</span>
            <input maxLength={20} placeholder="예: C-01" value={settings.location} onChange={(e) => setSettings({ ...settings, location: e.target.value })} className={`${inputCls} mt-1.5`} />
          </label>
          <button type="button" onClick={saveSettings} disabled={saving} className="px-6 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">저장</button>
        </div>
        <div className="space-y-3">
          <h3 className="text-xl font-normal text-slate-900 pb-3 border-b border-slate-900">수량 조정</h3>
          <p className="text-sm text-slate-600">실제 수량을 넣으면 차이만큼 조정되고 이력이 남습니다. (폐기·파손·매장 자체 구매 등)</p>
          <input type="number" min="0" placeholder="실제 수량" value={adjust.actual} onChange={(e) => setAdjust({ ...adjust, actual: e.target.value })} className={inputCls} />
          <input maxLength={100} placeholder="사유 (예: 파손 2개 폐기)" value={adjust.reason} onChange={(e) => setAdjust({ ...adjust, reason: e.target.value })} className={inputCls} />
          <button type="button" onClick={doAdjust} disabled={saving} className={btnOutline}>실제 수량으로 맞추기</button>
        </div>
      </section>

      <section>
        <h3 className="text-xl font-normal text-slate-900 pb-3 border-b border-slate-900">최근 이력</h3>
        {history.length === 0 ? (
          <p className="text-sm text-slate-400 py-6 text-center">이력이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-slate-200">
            {history.map((h) => (
              <li key={h.historyId} className="py-3 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="text-sm text-slate-900"><b className="font-semibold">{h.type}</b> · {h.userName}</p>
                  <p className="text-xs text-slate-500 truncate">{h.memo || '-'} · {formatDateTime(h.createdAt)}</p>
                </div>
                <div className="text-right shrink-0">
                  <p className={`text-sm font-semibold tabular-nums ${h.changeQty > 0 ? 'text-emerald-600' : 'text-rose-600'}`}>{h.changeQty > 0 ? `+${h.changeQty}` : h.changeQty}</p>
                  <p className="text-xs text-slate-400">→ {h.resultQty}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

/** /owner/store-stocks/new(등록) · /owner/store-stocks/:storeStockId(상세) */
export default function StoreStockOwnerDetailPage() {
  const { storeStockId } = useParams();
  return storeStockId === 'new' ? <RegisterView /> : <DetailView storeStockId={storeStockId} />;
}
