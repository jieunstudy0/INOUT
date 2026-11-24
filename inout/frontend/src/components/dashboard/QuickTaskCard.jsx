import { useNavigate } from 'react-router-dom';
import Spinner from '../common/Spinner';

/**
 * 대시보드 "빠른 업무" 카드 — 숫자와 작은 막대로 지금 상태를 보여 주고, 누르면 해당 화면으로 간다.
 * (기존 KPI 카드와 같은 흰 카드·아이콘 칸 모양)
 *
 * count: 처리할 건수. 0보다 크면 "처리 필요" 표시
 * bar:   { value, total, tone } — 비율 막대 (예: 부족 품목 / 전체 품목)
 * parts: [{ label, value, tone }] — 나눠 보는 막대 (예: 접수 3 · 처리 중 1)
 */
const TONE = {
  navy: { bar: 'bg-indigo-600', soft: 'bg-indigo-50', text: 'text-indigo-600' },
  teal: { bar: 'bg-emerald-500', soft: 'bg-emerald-100', text: 'text-emerald-600' },
  orange: { bar: 'bg-amber-500', soft: 'bg-amber-100', text: 'text-amber-600' },
  red: { bar: 'bg-rose-500', soft: 'bg-rose-50', text: 'text-rose-600' },
};

export default function QuickTaskCard({ title, count, unit = '건', sub, icon, tone = 'navy', href, loading, bar, parts }) {
  const navigate = useNavigate();
  const t = TONE[tone] || TONE.navy;
  const urgent = !loading && count > 0;

  return (
    <button type="button" onClick={() => navigate(href)}
      className="group text-left bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 hover:border-indigo-300 hover:shadow-md transition-all">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-x-1.5 gap-y-1 text-sm text-slate-700">
            {title}
            {urgent && <span className={`px-1.5 py-0.5 rounded-[10px] text-[10px] ${t.soft} ${t.text}`}>처리 필요</span>}
          </p>
          {loading ? (
            <div className="mt-3"><Spinner size="sm" /></div>
          ) : (
            <p className={`mt-1.5 text-2xl font-semibold tabular-nums ${urgent ? t.text : 'text-slate-900'}`}>
              {count == null ? '-' : count.toLocaleString('ko-KR')}
              <span className="ml-1 text-sm font-normal text-slate-500">{unit}</span>
            </p>
          )}
        </div>
        <span className={`hidden sm:flex w-11 h-11 rounded-xl items-center justify-center shrink-0 ${t.soft} ${t.text}`}>{icon}</span>
      </div>

      {!loading && bar && bar.total > 0 && (
        <div className="mt-3">
          <div className="h-1.5 rounded-full bg-slate-100 overflow-hidden">
            <div className={`h-full rounded-full ${(TONE[bar.tone] || t).bar}`} style={{ width: `${Math.min(100, (bar.value / bar.total) * 100)}%` }} />
          </div>
        </div>
      )}
      {!loading && parts && parts.some((p) => p.value > 0) && (
        <div className="mt-3 flex h-1.5 rounded-full bg-slate-100 overflow-hidden">
          {parts.map((p) => (
            <div key={p.label} className={(TONE[p.tone] || t).bar} style={{ flexGrow: p.value, flexBasis: 0 }} />
          ))}
        </div>
      )}

      <p className="mt-2 text-xs text-slate-500 line-clamp-2 sm:truncate">{loading ? ' ' : sub}</p>
      <p className="mt-2 hidden sm:flex items-center gap-1 text-xs text-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity">
        바로가기
        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" /></svg>
      </p>
    </button>
  );
}
