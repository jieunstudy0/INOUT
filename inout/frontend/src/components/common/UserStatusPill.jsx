import { USER_STATUS } from '../../utils/userStatus';

export default function UserStatusPill({ status }) {
  const s = USER_STATUS[status] || { label: status, cls: 'bg-slate-100 text-slate-500' };
  return <span className={`px-1.5 py-0.5 rounded-[10px] text-[11px] ${s.cls}`}>{s.label}</span>;
}
