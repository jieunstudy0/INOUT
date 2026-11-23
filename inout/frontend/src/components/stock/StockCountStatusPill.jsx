import { STOCK_COUNT_STATUS } from '../../utils/stockCount';

export default function StockCountStatusPill({ status }) {
  const meta = STOCK_COUNT_STATUS[status] || { label: status, cls: 'bg-slate-50 text-slate-600 border-slate-200' };
  return <span className={`px-2 py-0.5 text-[11px] font-semibold rounded-full border ${meta.cls}`}>{meta.label}</span>;
}
