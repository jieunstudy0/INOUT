import Avatar from './Avatar';
import StatusPill from './UserStatusPill';

/** 직원 카드 — 사진·이름·소속·상태. 누르면 상세 화면으로 간다. */
export default function EmployeeCard({ user, onClick, extra }) {
  return (
    <button type="button" onClick={onClick}
      className={`group text-left rounded-[10px] border border-slate-200 p-5 hover:border-indigo-300 transition-colors ${user.status === 'RESIGNED' ? 'opacity-60' : ''}`}>
      <div className="flex items-center gap-4">
        <Avatar src={user.profileImageUrl} name={user.name} size={64} />
        <div className="min-w-0">
          <p className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[15px] font-semibold text-slate-900 group-hover:underline underline-offset-4">{user.name}</span>
            <StatusPill status={user.status} />
            {user.isAdmin && <span className="px-1.5 py-0.5 rounded-[10px] text-[11px] bg-indigo-50 text-indigo-600">관리자</span>}
            {user.isLocked && <span className="px-1.5 py-0.5 rounded-[10px] text-[11px] bg-rose-50 text-rose-600">잠김</span>}
          </p>
          <p className="mt-1 text-sm text-slate-600 truncate">{user.storeName || '본사 (소속 없음)'}</p>
          <p className="text-xs text-slate-500 truncate">{user.email}</p>
          {extra && <p className="mt-1 text-xs text-slate-500">{extra}</p>}
        </div>
      </div>
    </button>
  );
}
