import Spinner from '../common/Spinner';

export default function DashboardPanel({ title, subtitle, action, loading, children, className = '' }) {
  return (
    <div className={`bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col ${className}`}>
      <div className="flex items-center justify-between mb-5 shrink-0 gap-3">
        <div className="min-w-0">
          <h3 className="text-sm font-bold text-slate-800">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400 mt-0.5 truncate">{subtitle}</p>}
        </div>
        {action}
      </div>
      {loading ? (
        <div className="flex-1 flex items-center justify-center py-8"><Spinner /></div>
      ) : (
        children
      )}
    </div>
  );
}

export function ProgressBar({ label, value, total, colorClass = 'bg-indigo-500', textClass = 'text-slate-600' }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-sm">
        <span className={`font-medium ${textClass}`}>{label}</span>
        <span className="text-slate-400 text-xs tabular-nums">
          {Number(value).toLocaleString('ko-KR')}건
          <span className="text-slate-300 mx-1">·</span>
          <span className={`font-semibold ${textClass}`}>{pct}%</span>
        </span>
      </div>
      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-700 ${colorClass}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
