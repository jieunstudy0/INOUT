import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  getStockDetail, updateStock, registerStock, deactivateStock, receiveStock, adjustStock,
  getCategories, uploadAdminImage,
} from '../api/stockApi';
import { toFieldErrors } from '../api/apiClient';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import PhotoField from '../components/common/PhotoField';
import { stockBadge } from '../utils/stockStatus';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-400';
const EMPTY = { name: '', categoryId: '', unitPrice: '', minStockLevel: '0', unitDescription: '', description: '', imageUrl: '' };
const TONE_CLS = { teal: 'bg-emerald-100 text-emerald-600', orange: 'bg-amber-100 text-amber-600', red: 'bg-rose-50 text-rose-600' };

function Field({ label, required, error, children }) {
  return (
    <label className="block">
      <span className="block text-sm text-slate-700 mb-1.5">{label}{required && <span className="text-rose-500 ml-0.5">*</span>}</span>
      {children}
      {error && <span className="block text-xs text-rose-600 mt-1">{error}</span>}
    </label>
  );
}

/**
 * 본사 상품 상세 — /admin/stock/new(등록) · /admin/stock/:itemId(수정)
 * 사진·기본 정보를 고쳐 저장하고, 같은 화면에서 입고·재고 조정·이력을 본다.
 */
export default function StockDetailAdmPage() {
  const { itemId } = useParams();
  const isNew = itemId === 'new';
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [detail, setDetail] = useState(null);
  const [categories, setCategories] = useState([]);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [receive, setReceive] = useState({ quantity: '', memo: '' });
  const [adjust, setAdjust] = useState({ actual: '', reason: '' });
  const [working, setWorking] = useState(false);

  const load = useCallback(() => {
    if (isNew) return;
    setLoading(true);
    getStockDetail(itemId, 0, 30)
      .then((d) => {
        setDetail(d);
        setForm({
          name: d.itemName || '',
          categoryId: d.categoryId != null ? String(d.categoryId) : '',
          unitPrice: d.unitPrice != null ? String(d.unitPrice) : '',
          minStockLevel: String(d.minStockLevel ?? 0),
          unitDescription: d.unitDescription || '',
          description: d.description || '',
          imageUrl: d.imageUrl || '',
        });
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [itemId, isNew]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { getCategories().then((c) => setCategories(c || [])).catch(() => {}); }, []);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const save = async (e) => {
    e.preventDefault();
    if (!form.imageUrl) { Toast.warning('상품 사진을 올려 주세요.'); return; }
    setSaving(true);
    setErrors({});
    const payload = {
      name: form.name.trim(),
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      unitPrice: form.unitPrice === '' ? null : Number(form.unitPrice),
      minStockLevel: Number(form.minStockLevel || 0),
      unitDescription: form.unitDescription.trim(),
      description: form.description.trim(),
      imageUrl: form.imageUrl,
    };
    try {
      if (isNew) {
        const newId = await registerStock(payload);
        Toast.success('상품을 등록했습니다.');
        navigate(`/admin/stock/${newId}`, { replace: true });
      } else {
        await updateStock(itemId, payload);
        Toast.success('상품 정보를 저장했습니다.');
        load();
      }
    } catch (err) {
      setErrors(toFieldErrors(err?.errors));
    } finally {
      setSaving(false);
    }
  };

  const doReceive = async () => {
    const qty = Number(receive.quantity);
    if (!qty || qty < 1) { Toast.warning('입고 수량을 1개 이상 입력해 주세요.'); return; }
    setWorking(true);
    try {
      await receiveStock(Number(itemId), qty, receive.memo.trim() || '본사 입고');
      Toast.success(`${qty}개를 입고했습니다.`);
      setReceive({ quantity: '', memo: '' });
      load();
    } catch { /* interceptor */ } finally { setWorking(false); }
  };

  const doAdjust = async () => {
    if (adjust.actual === '' || Number(adjust.actual) < 0) { Toast.warning('실제 수량을 0 이상으로 입력해 주세요.'); return; }
    if (!adjust.reason.trim()) { Toast.warning('조정 사유를 입력해 주세요.'); return; }
    setWorking(true);
    try {
      await adjustStock(Number(itemId), Number(adjust.actual), adjust.reason.trim());
      Toast.success('재고를 실제 수량으로 맞췄습니다.');
      setAdjust({ actual: '', reason: '' });
      load();
    } catch { /* interceptor */ } finally { setWorking(false); }
  };

  const doDeactivate = async () => {
    if (!window.confirm('이 상품을 비활성화할까요? 직원 상품 목록과 발주에서 빠집니다.')) return;
    try {
      await deactivateStock(itemId);
      Toast.success('비활성화했습니다.');
      navigate('/admin/stock');
    } catch { /* interceptor */ }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner size="lg" /></div>;

  const badge = detail ? stockBadge(detail.currentStock ?? 0, detail.minStockLevel ?? 0) : null;

  return (
    <div className="space-y-8 max-w-5xl">
      <div>
        <Link to="/admin/stock" className="text-sm text-slate-500 hover:text-slate-900">‹ 재고 관리</Link>
        <h2 className="mt-2 text-3xl font-normal text-slate-900">{isNew ? '신규 상품 등록' : form.name || '상품 상세'}</h2>
        {!isNew && detail?.deleted && <p className="mt-1 text-sm text-rose-600">비활성 상품입니다.</p>}
      </div>

      <form onSubmit={save} className="grid grid-cols-1 md:grid-cols-[320px_1fr] gap-8">
        <div>
          <p className="text-sm text-slate-700 mb-1.5">상품 사진<span className="text-rose-500 ml-0.5">*</span></p>
          <PhotoField value={form.imageUrl} onChange={(url) => setForm((f) => ({ ...f, imageUrl: url }))} upload={uploadAdminImage} />
        </div>

        <div className="space-y-4">
          <Field label="상품명" required error={errors.name}>
            <input value={form.name} onChange={set('name')} className={inputCls} placeholder="예: 원두 블렌드 1kg" />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="카테고리" required error={errors.categoryId}>
              <select value={form.categoryId} onChange={set('categoryId')} className={inputCls}>
                <option value="">선택</option>
                {categories.map((c) => <option key={c.categoryId} value={c.categoryId}>{c.name}</option>)}
              </select>
            </Field>
            <Field label="단가 (원)" required error={errors.unitPrice}>
              <input type="number" min="0" value={form.unitPrice} onChange={set('unitPrice')} className={`${inputCls} text-right`} />
            </Field>
            <Field label="안전재고" error={errors.minStockLevel}>
              <input type="number" min="0" value={form.minStockLevel} onChange={set('minStockLevel')} className={`${inputCls} text-right`} />
            </Field>
            <Field label="단위" error={errors.unitDescription}>
              <input value={form.unitDescription} onChange={set('unitDescription')} className={inputCls} placeholder="예: EA, 박스, kg" />
            </Field>
          </div>
          <Field label="상품 설명" error={errors.description}>
            <textarea rows={4} value={form.description} onChange={set('description')} className={`${inputCls} resize-none`} />
          </Field>
          <div className="flex items-center gap-2 pt-2">
            <button type="submit" disabled={saving}
              className="px-6 py-2.5 text-sm font-semibold rounded-full bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
              {saving ? '저장 중...' : isNew ? '등록' : '저장'}
            </button>
            <Link to="/admin/stock" className="px-5 py-2.5 text-sm rounded-full border border-slate-300 text-slate-700 hover:bg-slate-50">취소</Link>
            {!isNew && !detail?.deleted && (
              <button type="button" onClick={doDeactivate} className="ml-auto text-sm text-slate-500 hover:text-rose-600">상품 비활성화</button>
            )}
          </div>
        </div>
      </form>

      {!isNew && detail && (
        <section className="space-y-5">
          <h3 className="text-xl font-normal text-slate-900 pb-3 border-b border-slate-900">본사 재고</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="rounded-[10px] bg-slate-50 p-5">
              <p className="text-sm text-slate-600">지금 재고</p>
              <p className="mt-1 text-3xl font-semibold text-slate-900 tabular-nums">
                {(detail.currentStock ?? 0).toLocaleString()}<span className="text-base font-normal text-slate-500 ml-1">{form.unitDescription || '개'}</span>
              </p>
              <p className="mt-2 flex items-center gap-2 text-xs text-slate-500">
                <span className={`px-1.5 py-0.5 rounded-[10px] ${TONE_CLS[badge.tone]}`}>{badge.label}</span>
                안전재고 {detail.minStockLevel} · 누적 입고 {detail.totalReceived ?? 0} · 사용 {detail.totalUsed ?? 0}
              </p>
            </div>
            <div className="rounded-[10px] border border-slate-200 p-5 space-y-2">
              <p className="text-sm font-semibold text-slate-900">입고</p>
              <input type="number" min="1" placeholder="입고 수량" value={receive.quantity}
                onChange={(e) => setReceive((r) => ({ ...r, quantity: e.target.value }))} className={inputCls} />
              <input placeholder="메모 (선택)" value={receive.memo} onChange={(e) => setReceive((r) => ({ ...r, memo: e.target.value }))} className={inputCls} />
              <button type="button" disabled={working} onClick={doReceive}
                className="w-full py-2 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50 disabled:opacity-50">입고 처리</button>
            </div>
            <div className="rounded-[10px] border border-slate-200 p-5 space-y-2">
              <p className="text-sm font-semibold text-slate-900">재고 조정 (실사)</p>
              <input type="number" min="0" placeholder="실제 수량" value={adjust.actual}
                onChange={(e) => setAdjust((a) => ({ ...a, actual: e.target.value }))} className={inputCls} />
              <input placeholder="조정 사유" value={adjust.reason} onChange={(e) => setAdjust((a) => ({ ...a, reason: e.target.value }))} className={inputCls} />
              <button type="button" disabled={working} onClick={doAdjust}
                className="w-full py-2 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50 disabled:opacity-50">실제 수량으로 맞추기</button>
            </div>
          </div>

          <div>
            <p className="text-sm font-semibold text-slate-900 mb-2">최근 이력</p>
            {(detail.history || []).length === 0 ? (
              <p className="text-sm text-slate-400 py-6 text-center rounded-[10px] bg-slate-50">이력이 없습니다.</p>
            ) : (
              <div className="rounded-[10px] border border-slate-200 overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead className="bg-slate-50 text-xs text-slate-500">
                    <tr><th className="px-4 py-2.5 text-left">일시</th><th className="px-4 py-2.5 text-left">구분</th><th className="px-4 py-2.5 text-right">수량</th><th className="px-4 py-2.5 text-right">처리 후</th><th className="px-4 py-2.5 text-left">메모</th><th className="px-4 py-2.5 text-left">처리자</th></tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {detail.history.map((h, i) => (
                      <tr key={i}>
                        <td className="px-4 py-2.5 text-slate-500 whitespace-nowrap">{h.date ? new Date(h.date).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '-'}</td>
                        <td className="px-4 py-2.5">{h.type}</td>
                        <td className={`px-4 py-2.5 text-right tabular-nums ${h.quantity > 0 ? 'text-emerald-600' : 'text-rose-600'}`}>{h.quantity > 0 ? `+${h.quantity}` : h.quantity}</td>
                        <td className="px-4 py-2.5 text-right tabular-nums">{h.resultStock ?? '-'}</td>
                        <td className="px-4 py-2.5 text-slate-600">{h.remarks || '-'}</td>
                        <td className="px-4 py-2.5 text-slate-600">{h.workerName || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
