import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getNotifications, markNotificationRead, markAllNotificationsRead } from '../api/notificationApi';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 20;

function timeAgo(value) {
  if (!value) return '';
  const diff = (Date.now() - new Date(value).getTime()) / 1000;
  if (diff < 60) return '방금';
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  if (diff < 86400 * 7) return `${Math.floor(diff / 86400)}일 전`;
  return new Date(value).toLocaleDateString('ko-KR');
}

const notifyBell = () => window.dispatchEvent(new Event('inout:notifications-changed'));

/** 내 알림 목록 (모든 역할 공용). 누르면 읽음 처리하고 관련 화면으로 이동한다. */
export default function NotificationsPage() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = useCallback((p) => {
    setLoading(true);
    getNotifications(p, PAGE_SIZE)
      .then((data) => {
        const content = data?.content || [];
        setItems((prev) => (p === 0 ? content : [...prev, ...content]));
        setLast(data?.last ?? true);
        setPage(p);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(0); }, [load]);

  const open = async (n) => {
    if (!n.read) {
      try {
        await markNotificationRead(n.notificationId);
        setItems((prev) => prev.map((x) => (x.notificationId === n.notificationId ? { ...x, read: true } : x)));
        notifyBell();
      } catch {
        /* interceptor */
      }
    }
    if (n.linkUrl) navigate(n.linkUrl);
  };

  const readAll = async () => {
    try {
      await markAllNotificationsRead();
      setItems((prev) => prev.map((x) => ({ ...x, read: true })));
      notifyBell();
    } catch {
      /* interceptor */
    }
  };

  const unread = items.filter((n) => !n.read).length;

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">알림</h2>
          <p className="text-sm text-slate-500 mt-0.5">최근 90일 알림을 보여 줍니다.</p>
        </div>
        {unread > 0 && (
          <button onClick={readAll} className="px-3 py-2 text-xs font-semibold rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50">
            모두 읽음
          </button>
        )}
      </div>

      {loading && items.length === 0 ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : items.length === 0 ? (
        <EmptyState message="받은 알림이 없습니다." />
      ) : (
        <ul className="bg-white rounded-2xl border border-slate-200 shadow-sm divide-y divide-slate-100 overflow-hidden">
          {items.map((n) => (
            <li key={n.notificationId}>
              <button onClick={() => open(n)} className={`w-full text-left px-4 py-3 flex gap-3 hover:bg-slate-50 ${n.read ? '' : 'bg-indigo-50/40'}`}>
                <span className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${n.read ? 'bg-transparent' : 'bg-indigo-500'}`} />
                <span className="min-w-0 flex-1">
                  <span className={`block text-sm ${n.read ? 'text-slate-600' : 'font-semibold text-slate-800'}`}>{n.title}</span>
                  {n.message && <span className="block text-xs text-slate-500 mt-0.5">{n.message}</span>}
                  <span className="block text-[11px] text-slate-400 mt-1">{timeAgo(n.createdAt)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {!last && (
        <button onClick={() => load(page + 1)} disabled={loading}
          className="w-full py-2.5 text-sm font-semibold rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-50">
          {loading ? '불러오는 중...' : '더 보기'}
        </button>
      )}
    </div>
  );
}
