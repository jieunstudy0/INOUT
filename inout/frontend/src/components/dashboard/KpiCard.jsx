import { useNavigate } from 'react-router-dom';
import Spinner from '../common/Spinner';

/**
 * 통일 KPI 카드 — indigo 계열 디자인 토큰
 * accent: indigo | emerald | sky | amber | rose | violet | teal
 */
const ACCENT = {
  indigo:  { blob: 'bg-indigo-400', iconWrap: 'bg-indigo-100', icon: 'text-indigo-600' },
  emerald: { blob: 'bg-emerald-400', iconWrap: 'bg-emerald-100', icon: 'text-emerald-600' },
  sky:     { blob: 'bg-sky-400', iconWrap: 'bg-sky-100', icon: 'text-sky-600' },
  amber:   { blob: 'bg-amber-400', iconWrap: 'bg-amber-100', icon: 'text-amber-600' },
  rose:    { blob: 'bg-rose-400', iconWrap: 'bg-rose-100', icon: 'text-rose-600' },
  violet:  { blob: 'bg-violet-400', iconWrap: 'bg-violet-100', icon: 'text-violet-600' },
  teal:    { blob: 'bg-teal-400', iconWrap: 'bg-teal-100', icon: 'text-teal-600' },
};

export default function KpiCard({
  title,
  value,
  sub,
  icon,
  accent = 'indigo',
  href,
  loading,
  action,
}) {
  const navigate = useNavigate();
  const cfg = ACCENT[accent] || ACCENT.indigo;

  return (
    <div
      className={`relative bg-white rounded-2xl border border-slate-200 shadow-sm p-5 overflow-hidden
                  group transition-all duration-200 hover:shadow-md hover:-translate-y-0.5
                  ${href ? 'cursor-pointer' : ''}`}
      onClick={href ? () => navigate(href) : undefined}
    >
      <div className={`absolute -right-3 -top-3 w-20 h-20 rounded-full opacity-10 ${cfg.blob}`} />
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</p>
          {loading ? (
            <div className="mt-3"><Spinner size="sm" /></div>
          ) : (
            <p className="mt-2 text-2xl font-extrabold text-slate-800 tabular-nums leading-tight break-all">
              {typeof value === 'number' ? value.toLocaleString('ko-KR') : value}
            </p>
          )}
          {sub && !loading && <p className="mt-1.5 text-xs text-slate-400 truncate">{sub}</p>}
          {action && !loading && <div className="mt-3" onClick={(e) => e.stopPropagation()}>{action}</div>}
        </div>
        <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${cfg.iconWrap}`}>
          <span className={cfg.icon}>{icon}</span>
        </div>
      </div>
      {href && (
        <div className="mt-3 flex items-center gap-1 text-xs font-medium text-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity">
          바로가기
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </div>
      )}
    </div>
  );
}
