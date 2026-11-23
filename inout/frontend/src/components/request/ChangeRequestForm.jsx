import { useState } from 'react';
import { createChangeRequest } from '../../api/changeRequestApi';
import { Toast } from '../../utils/toast';
import { toFieldErrors } from '../../api/apiClient';
import { categoriesFor } from '../../utils/changeRequest';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40';

/**
 * 수정 요청 작성 창 (휴대폰에서는 아래에서 올라오는 시트).
 * preset: { category, refType, refId, refLabel, title, content } — 다른 화면에서 열 때 미리 채운다.
 */
export default function ChangeRequestForm({ role = 'emp', preset = {}, onClose, onDone }) {
  const categories = categoriesFor(role);
  const [form, setForm] = useState({
    category: preset.category || categories[0].value,
    title: preset.title || '',
    content: preset.content || '',
  });
  const [photo, setPhoto] = useState(null);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const selected = categories.find((c) => c.value === form.category);

  const submit = async () => {
    setSaving(true);
    setErrors({});
    try {
      await createChangeRequest({
        category: form.category,
        refType: preset.refType || 'NONE',
        refId: preset.refId ?? null,
        title: form.title,
        content: form.content,
      }, photo);
      Toast.success(selected?.handler === 'HQ' ? '본사에 요청을 보냈습니다.' : '점주에게 요청을 보냈습니다.');
      onDone?.();
      onClose();
    } catch (err) {
      setErrors(toFieldErrors(err?.errors));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center">
      <div className="absolute inset-0 bg-slate-900/40" onClick={onClose} />
      <div className="relative bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-5 py-3 border-b border-slate-100">
          <h3 className="text-sm font-bold text-slate-800">수정 요청</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 px-2">닫기</button>
        </div>
        <div className="p-5 space-y-3">
          {preset.refLabel && (
            <div className="px-3 py-2 rounded-xl bg-slate-50 text-xs text-slate-600">대상: <b>{preset.refLabel}</b></div>
          )}
          <div>
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
              disabled={Boolean(preset.category)} className={inputCls}>
              {categories.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
            <p className="text-[11px] text-slate-400 mt-1">받는 곳: {selected?.handler === 'HQ' ? '본사' : '우리 매장 점주'}</p>
          </div>
          <div>
            <input type="text" maxLength={100} placeholder="제목 (예: 원두 2봉 덜 옴)" value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })} className={inputCls} />
            {errors.title && <p className="text-xs text-rose-600 mt-1">{errors.title}</p>}
          </div>
          <div>
            <textarea rows={5} maxLength={1000} placeholder="무엇을 어떻게 고쳐 달라는지 적어 주세요." value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })} className={`${inputCls} resize-none`} />
            {errors.content && <p className="text-xs text-rose-600 mt-1">{errors.content}</p>}
          </div>
          <label className="block">
            <span className="text-xs font-semibold text-slate-600">사진 (선택)</span>
            <input type="file" accept="image/jpeg,image/png,image/webp" capture="environment"
              onChange={(e) => setPhoto(e.target.files?.[0] || null)}
              className="block w-full mt-1 text-xs text-slate-600 file:mr-3 file:px-3 file:py-2 file:rounded-lg file:border-0 file:bg-slate-100" />
          </label>
          <button onClick={submit} disabled={saving}
            className="w-full py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
            {saving ? '보내는 중...' : '요청 보내기'}
          </button>
        </div>
      </div>
    </div>
  );
}
