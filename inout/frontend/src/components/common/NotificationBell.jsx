import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getUnreadNotificationCount } from '../../api/notificationApi';

const POLL_MS = 60_000;

/**
 * 헤더 알림 종. 안 읽은 개수를 1분마다, 그리고 화면을 옮길 때마다 다시 읽는다.
 * basePath: '/admin' | '/emp' | '/owner' — 누르면 {basePath}/notifications 로 이동한다.
 */
export default function NotificationBell({ basePath }) {
  const [count, setCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();

  const refresh = useCallback(() => {
    getUnreadNotificationCount()
      .then((n) => setCount(Number(n) || 0))
      .catch(() => {});
  }, []);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, POLL_MS);
    const onFocus = () => refresh();
    window.addEventListener('focus', onFocus);
    window.addEventListener('inout:notifications-changed', onFocus);
    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('inout:notifications-changed', onFocus);
    };
  }, [refresh, location.pathname]);

  return (
    <button type="button" onClick={() => navigate(`${basePath}/notifications`)}
      aria-label={count > 0 ? `안 읽은 알림 ${count}개` : '알림'}
      className="relative p-2 rounded-xl text-slate-500 hover:bg-slate-100 hover:text-slate-800">
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
      </svg>
      {count > 0 && (
        <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </button>
  );
}
