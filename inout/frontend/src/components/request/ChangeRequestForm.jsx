import { useState } from 'react';
import { createChangeRequest } from '../../api/changeRequestApi';
import { Toast } from '../../utils/toast';
import { toFieldErrors } from '../../api/apiClient';
import { categoriesFor, DELIVERY_ISSUE_TYPES } from '../../utils/changeRequest';

const inputCls = 'w-full px-3 py-2.5 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40';

/**
 * 수정 요청 작성 창 (휴대폰에서는 아래에서 올라오는 시트).
 * preset: { category, issueType, refType, refId, refLabel, title, detail } — 다른 화면에서 열 때 미리 채운다.
 *   category를 주면 종류를 바꿀 수 없다 (예: 수령 확인 화면의 배송 이상 신고). 이때는 고를 칸 대신 글자로 보여 준다.
 *   detail: 수량 부족일 때 내용 칸에 넣을 덜 받은 품목 목록
 *
 * 배송 이상은 세부 종류(수량 부족·파손·오배송·기타)를 탭으로 고른다.
 * 제목·내용은 직원이 직접 고치기 전까지 고른 종류에 맞춰 바뀐다.
 */
export default function ChangeRequestForm({ role = 'emp', preset = {}, onClose, onDone }) {
  const categories = categoriesFor(role);
  const categoryFixed = Boolean(preset.category);

  const issueOf = (value) => DELIVERY_ISSUE_TYPES.find((t) => t.value === value);
  const autoTitle = (type) => {
    const label = issueOf(type)?.label;
    if (!label) return preset.title || '';
    return preset.refLabel ? `${preset.refLabel} ${label}` : `배송 ${label}`;
  };
  const autoContent = (type) => {
    if (type === 'SHORTAGE' && preset.detail) return `덜 온 품목·수량:\n${preset.detail}\n\n상황: `;
    return issueOf(type)?.template ?? '';
  };

  const initialIssue = preset.issueType || null;
  const [form, setForm] = useState({
    category: preset.category || categories[0].value,
    issueType: initialIssue,
    title: preset.title || autoTitle(initialIssue),
    content: initialIssue ? autoContent(initialIssue) : (preset.content || ''),
  });
  // 직원이 직접 고친 칸은 종류를 바꿔도 덮어쓰지 않는다
  const [touched, setTouched] = useState({ title: Boolean(preset.title), content: false });
  const [photo, setPhoto] = useState(null);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const selected = categories.find((c) => c.value === form.category);
  const isDelivery = form.category === 'DELIVERY_ISSUE';
  const photoRecommended = isDelivery && ['DAMAGED', 'WRONG_ITEM'].includes(form.issueType);

  const pickIssue = (type) => setForm((f) => ({
    ...f,
    issueType: type,
    title: touched.title ? f.title : autoTitle(type),
    content: touched.content ? f.content : autoContent(type),
  }));

  const submit = async () => {
    if (isDelivery && !form.issueType) {
      Toast.warning('어떤 배송 문제인지 골라 주세요.');
      return;
    }
    setSaving(true);
    setErrors({});
    try {
      await createChangeRequest({
        category: form.category,
        issueType: isDelivery ? form.issueType : null,
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
          <h3 className="text-sm font-bold text-slate-800">{isDelivery ? '배송 이상 신고' : '수정 요청'}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 px-2">닫기</button>
        </div>
        <div className="p-5 space-y-4">
          {preset.refLabel && (
            <div className="px-3 py-2 rounded-xl bg-slate-50 text-xs text-slate-600">대상: <b>{preset.refLabel}</b></div>
          )}

          {categoryFixed ? (
            <div className="flex items-center justify-between gap-2 text-xs text-slate-600">
              <span>요청 종류 <b className="text-slate-800">{selected?.label.replace(/ \(.*\)$/, '')}</b></span>
              <span>받는 곳 <b className="text-slate-800">{selected?.handler === 'HQ' ? '본사' : '우리 매장 점주'}</b></span>
            </div>
          ) : (
            <div>
              <label htmlFor="cr-category" className="text-xs font-semibold text-slate-600">요청 종류</label>
              <select id="cr-category" value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                className={`${inputCls} mt-1 bg-white`}>
                {categories.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
              <p className="text-[11px] text-slate-400 mt-1">받는 곳: {selected?.handler === 'HQ' ? '본사' : '우리 매장 점주'}</p>
            </div>
          )}

          {isDelivery && (
            <div>
              <p className="text-xs font-semibold text-slate-600 mb-1.5">어떤 문제인가요?</p>
              <div role="tablist" aria-label="배송 이상 종류" className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {DELIVERY_ISSUE_TYPES.map((t) => {
                  const on = form.issueType === t.value;
                  return (
                    <button key={t.value} type="button" role="tab" aria-selected={on} onClick={() => pickIssue(t.value)}
                      className={`px-2 py-2.5 rounded-xl border text-left transition-colors ${
                        on ? 'border-indigo-600 bg-indigo-50 text-indigo-700' : 'border-slate-200 text-slate-700 hover:bg-slate-50'}`}>
                      <span className="block text-sm font-semibold">{t.label}</span>
                      <span className={`block text-[11px] mt-0.5 ${on ? 'text-indigo-600' : 'text-slate-400'}`}>{t.hint}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div>
            <input type="text" maxLength={100} placeholder={isDelivery ? '제목 (예: 원두 2봉 덜 옴)' : '제목'} value={form.title}
              onChange={(e) => { setTouched((t) => ({ ...t, title: true })); setForm({ ...form, title: e.target.value }); }}
              className={inputCls} />
            {errors.title && <p className="text-xs text-rose-600 mt-1">{errors.title}</p>}
          </div>
          <div>
            <textarea rows={5} maxLength={1000} placeholder="무엇을 어떻게 고쳐 달라는지 적어 주세요." value={form.content}
              onChange={(e) => { setTouched((t) => ({ ...t, content: true })); setForm({ ...form, content: e.target.value }); }}
              className={`${inputCls} resize-none`} />
            {errors.content && <p className="text-xs text-rose-600 mt-1">{errors.content}</p>}
          </div>
          <label className="block">
            <span className="text-xs font-semibold text-slate-600">
              사진 {photoRecommended ? <b className="text-rose-600">(올려 주시면 본사가 바로 확인할 수 있어요)</b> : '(선택)'}
            </span>
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
