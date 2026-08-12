const PRESETS = {
  // 발주
  REQUESTED: { label: '직원 기안', cls: 'bg-slate-100 text-slate-600' },
  ORDERED:   { label: '본사대기', cls: 'bg-blue-100 text-blue-700' },
  PAID:      { label: '본사대기', cls: 'bg-amber-100 text-amber-700' },
  PARTIAL:   { label: '부분승인', cls: 'bg-sky-100 text-sky-700' },
  APPROVED:  { label: '승인',     cls: 'bg-emerald-100 text-emerald-700' },
  COMPLETED: { label: '완료',     cls: 'bg-emerald-100 text-emerald-700' },
  REJECTED:  { label: '반려',     cls: 'bg-rose-100 text-rose-700' },
  CANCELLED: { label: '취소',     cls: 'bg-slate-100 text-slate-500' },
  // 연차
  PENDING:   { label: '승인대기', cls: 'bg-amber-100 text-amber-700' },
  // 배송
  READY:     { label: '배송준비', cls: 'bg-slate-100 text-slate-600' },
  SHIPPING:  { label: '배송중',   cls: 'bg-sky-100 text-sky-700' },
  DELIVERED: { label: '배송완료', cls: 'bg-emerald-100 text-emerald-700' },
  // 문의
  WAITING:   { label: '답변대기', cls: 'bg-rose-100 text-rose-600' },
  READ:      { label: '확인완료', cls: 'bg-emerald-100 text-emerald-700' },
};

export default function StatusBadge({ status, label, className = '' }) {
  const cfg = PRESETS[status] || { label: label || status || '-', cls: 'bg-slate-100 text-slate-600' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold ${cfg.cls} ${className}`}>
      {label || cfg.label}
    </span>
  );
}
