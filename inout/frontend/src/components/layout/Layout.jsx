import { useEffect, useState, useCallback } from 'react';
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import NotificationBell from '../common/NotificationBell';
import InstallAppBanner from '../common/InstallAppBanner';
import TopNav from './TopNav';
import { EMP_MENU, ADMIN_MENU } from './navMenus';
import { logout } from '../../api/authApi';
import { getMyProfile } from '../../api/profileApi';
import { getDashboardSummary } from '../../api/dashboardApi';
import { getEmpDashboardSummary } from '../../api/dashboardEmpApi';
import { getAdminDepositList } from '../../api/adminDepositApi';
import {
  subscribeHeaderRefresh,
  formatWon,
  parseJwtPayload,
  resolveRoleFromPayload,
} from '../../utils/headerSync';

/** 본사(ADMIN)·직원(EMPLOYEE) 공용 레이아웃: 상단 메뉴 + 가운데 본문 */
export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [role, setRole] = useState('EMPLOYEE');
  const [displayName, setDisplayName] = useState('사용자');
  const [storeName, setStoreName] = useState('');
  const [unreadInquiryCount, setUnreadInquiryCount] = useState(0);
  const [totalDeposit, setTotalDeposit] = useState(null);
  const [empDepositBalance, setEmpDepositBalance] = useState(null);
  const [remainingLeaveDays, setRemainingLeaveDays] = useState(null);
  const [cartCount, setCartCount] = useState(0);

  const loadHeaderMeta = useCallback(async () => {
    const payload = parseJwtPayload();
    if (!payload) {
      navigate('/login', { replace: true });
      return;
    }

    const nextRole = resolveRoleFromPayload(payload);
    setRole(nextRole);

    try {
      const profile = await getMyProfile();
      setDisplayName(profile?.name || payload.sub || payload.email || '사용자');
      setStoreName(profile?.storeName || (nextRole === 'ADMIN' ? '본사' : '매장'));
    } catch {
      setDisplayName(payload.sub || payload.email || '사용자');
      setStoreName(nextRole === 'ADMIN' ? '본사' : '매장');
    }

    if (nextRole === 'ADMIN') {
      try {
        const [dash, deposit] = await Promise.all([
          getDashboardSummary(),
          getAdminDepositList({ page: 0, size: 1 }).catch(() => null),
        ]);
        setUnreadInquiryCount(dash?.unreadInquiryCount ?? dash?.waitingCsInquiryCount ?? 0);
        setTotalDeposit(deposit?.summary?.totalBalance ?? deposit?.totalBalance ?? null);
        if (dash?.userName) setDisplayName(dash.userName);
      } catch {
        /* ignore */
      }
    } else {
      try {
        const dash = await getEmpDashboardSummary();
        if (dash?.userName) setDisplayName(dash.userName);
        if (dash?.storeName) setStoreName(dash.storeName);
        setCartCount(dash?.cartItemCount ?? 0);
        setEmpDepositBalance(dash?.depositBalance != null ? Number(dash.depositBalance) : null);
        setRemainingLeaveDays(dash?.remainingLeaveDays != null ? Number(dash.remainingLeaveDays) : null);
      } catch {
        /* ignore */
      }
    }
  }, [navigate]);

  useEffect(() => {
    if (location.pathname === '/login') return;
    loadHeaderMeta();
  }, [location.pathname, loadHeaderMeta]);

  useEffect(() => subscribeHeaderRefresh(() => loadHeaderMeta()), [loadHeaderMeta]);

  const handleLogout = async () => {
    if (!window.confirm('로그아웃 하시겠습니까?')) return;
    try {
      await logout();
    } catch {
      /* ignore */
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/login', { replace: true });
    }
  };

  const isAdmin = role === 'ADMIN';
  const menu = isAdmin ? ADMIN_MENU : EMP_MENU;
  const basePath = isAdmin ? '/admin' : '/emp';

  // 맨 위 얇은 줄 — 참고 화면의 "이벤트 · 언어 · 회원가입" 자리에 소속·주요 수치를 둔다.
  const utility = isAdmin ? (
    <>
      <span>본사 관리자</span>
      {totalDeposit != null && <span>전사 예치금 <b className="font-semibold text-slate-900 tabular-nums">{formatWon(totalDeposit)}</b></span>}
      <Link to="/admin/inquiries" className="hover:underline underline-offset-2">
        미처리 문의 <b className="font-semibold text-slate-900">{unreadInquiryCount}건</b>
      </Link>
      <span className="text-slate-900">{displayName} 님</span>
    </>
  ) : (
    <>
      <span>{storeName || '매장'}</span>
      <Link to="/emp/leaves" className="hover:underline underline-offset-2">
        잔여 연차 <b className="font-semibold text-slate-900">{remainingLeaveDays == null ? '-' : `${remainingLeaveDays}일`}</b>
      </Link>
      <Link to="/emp/deposit" className="hover:underline underline-offset-2">
        매장 예치금 <b className="font-semibold text-slate-900 tabular-nums">{empDepositBalance == null ? '-' : formatWon(empDepositBalance)}</b>
      </Link>
      <span className="text-slate-900">{displayName} 님</span>
    </>
  );

  const cartButton = !isAdmin && (
    <Link to="/emp/cart" title="장바구니" className="relative p-2 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-50">
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.45L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
      </svg>
      {cartCount > 0 && (
        <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">
          {cartCount > 9 ? '9+' : cartCount}
        </span>
      )}
    </Link>
  );

  const logoutButton = (
    <button type="button" onClick={handleLogout}
      className="px-4 py-1.5 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50">
      로그아웃
    </button>
  );

  return (
    <div className="min-h-screen bg-white">
      <TopNav
        menu={menu}
        utility={utility}
        actions={<>{cartButton}<NotificationBell basePath={basePath} />{logoutButton}</>}
        mobileActions={<>{cartButton}<NotificationBell basePath={basePath} />{logoutButton}</>}
      />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 sm:py-8">
        {!isAdmin && <InstallAppBanner />}
        <Outlet />
      </main>
    </div>
  );
}
