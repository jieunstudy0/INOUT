import EmptyState from '../common/EmptyState';
import Spinner from '../common/Spinner';
import StatusBadge from './StatusBadge';

/**
 * 3구역 공통 최근 활동 테이블
 * columns: [{ key, label, align?, render? }]
 * rows: array of objects
 */
export default function RecentActivityTable({
  title,
  subtitle,
  columns,
  rows = [],
  loading,
  emptyMessage = '최근 데이터가 없습니다.',
  action,
  onRowClick,
}) {
  return (
    <section className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-bold text-slate-800">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
        </div>
        {action}
      </div>

      {loading ? (
        <div className="p-10 flex justify-center"><Spinner /></div>
      ) : rows.length === 0 ? (
        <div className="p-4"><EmptyState message={emptyMessage} /></div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-slate-500 text-xs">
              <tr>
                {columns.map((col) => (
                  <th
                    key={col.key}
                    className={`px-4 py-2.5 font-semibold ${col.align === 'right' ? 'text-right' : 'text-left'}`}
                  >
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((row, idx) => (
                <tr
                  key={row.id ?? row.key ?? idx}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={`hover:bg-slate-50/80 transition-colors ${onRowClick ? 'cursor-pointer' : ''}`}
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className={`px-4 py-3 ${col.align === 'right' ? 'text-right' : 'text-left'} ${col.className || ''}`}
                    >
                      {col.render ? col.render(row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export { StatusBadge };
