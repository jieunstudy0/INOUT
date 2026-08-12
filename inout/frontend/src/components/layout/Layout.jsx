import { useEffect, useState, useCallback } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
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

const ADMIN_NAV_ITEMS = [
  { path: '/admin/dashboard', label: '대시보드' },
  { path: '/admin/orders', label: '발주 관리' },
  { path: '/admin/delivery', label: '배송 관리' },
  { path: '/admin/stock', label: '재고 관리' },
  { path: '/admin/users', label: '직원 관리' },
  { path: '/admin/leaves', label: '연차 현황' },
  { path: '/admin/inquiries', label: '문의 사항' },
  { path: '/admin/deposit', label: '예치금 관리' },
];

const EMP_NAV_ITEMS = [
  { path: '/emp/dashboard', label: '대시보드' },
  { path: '/emp/stocks', label: '재고 조회' },
  { path: '/emp/stock-use', label: '재고 사용' },
  { path: '/emp/orders', label: '발주 내역' },
  { path: '/emp/cart', label: '장바구니' },
  { path: '/emp/leaves', label: '연차 관리' },
  { path: '/emp/inquiries', label: '문의 사항' },
  { path: '/emp/deposit', label: '매장 예치금' },
  { path: '/emp/profile', label: '내 정보' },
];

const PAGE_TITLE_MAP = {
  '/admin/dashboard': '대시보드 (관리자)',
  '/admin/orders': '발주 관리',
  '/admin/stock': '재고 관리',
  '/admin/users': '직원 관리',
  '/admin/leaves': '연차 현황',
  '/admin/inquiries': '문의 사항',
  '/admin/deposit': '예치금 관리',
  '/admin/delivery': '배송 관리',
  '/emp/dashboard': '대시보드',
  '/emp/orders': '내 발주 내역',
  '/emp/cart': '장바구니',
  '/emp/stocks': '재고 조회',
  '/emp/stock-use': '재고 사용 처리',
  '/emp/leaves': '연차 관리',
  '/emp/leaves/register': '연차 신청',
  '/emp/inquiries': '문의 사항',
  '/emp/profile': '내 정보',
  '/emp/deposit': '매장 예치금',
};

function iconDashboard() {
  return (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
    </svg>
  );
}

const ADMIN_ICONS = {
  '/admin/dashboard': iconDashboard(),
  '/admin/orders': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
    </svg>
  ),
  '/admin/delivery': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
    </svg>
  ),
  '/admin/stock': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
    </svg>
  ),
  '/admin/users': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
    </svg>
  ),
  '/admin/leaves': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
    </svg>
  ),
  '/admin/inquiries': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z" />
    </svg>
  ),
  '/admin/deposit': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a2.25 2.25 0 00-2.25-2.25H15a3 3 0 11-6 0H5.25A2.25 2.25 0 003 12m18 0v6a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18v-6m18 0V9M3 12V9m18 0a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 9m18 0V6a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 6v3" />
    </svg>
  ),
};

const EMP_ICONS = {
  '/emp/dashboard': iconDashboard(),
  '/emp/stocks': ADMIN_ICONS['/admin/stock'],
  '/emp/stock-use': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5m.75-9l3-3 2.148 2.148A12.061 12.061 0 0116.5 7.605" />
    </svg>
  ),
  '/emp/orders': ADMIN_ICONS['/admin/orders'],
  '/emp/cart': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.45L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
    </svg>
  ),
  '/emp/inquiries': ADMIN_ICONS['/admin/inquiries'],
  '/emp/leaves': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
    </svg>
  ),
  '/emp/deposit': ADMIN_ICONS['/admin/deposit'],
  '/emp/profile': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
    </svg>
  ),
};

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
        setEmpDepositBalance(
          dash?.depositBalance != null ? Number(dash.depositBalance) : null,
        );
        setRemainingLeaveDays(
          dash?.remainingLeaveDays != null ? Number(dash.remainingLeaveDays) : null,
        );
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
  const initial = (displayName || '?').charAt(0);
  const pageTitle = PAGE_TITLE_MAP[location.pathname]
    || (location.pathname.startsWith('/admin/inquiries/') ? '문의 상세'
      : location.pathname.startsWith('/admin/orders/') ? '발주 상세'
        : location.pathname.startsWith('/emp/payment/') ? '발주 결제'
          : location.pathname.startsWith('/emp/inquiries/') ? '문의 상세'
            : location.pathname.startsWith('/emp/leaves/') ? '연차 상세'
              : 'INOUT');
  const activeNavItems = isAdmin ? ADMIN_NAV_ITEMS : EMP_NAV_ITEMS;
  const iconMap = isAdmin ? ADMIN_ICONS : EMP_ICONS;
  const accentLogo = isAdmin
    ? 'w-8 h-8 bg-indigo-500 rounded-lg flex items-center justify-center mr-3 shadow-lg shadow-indigo-900/50'
    : 'w-8 h-8 bg-emerald-500 rounded-lg flex items-center justify-center mr-3 shadow-lg shadow-emerald-900/50';

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 w-64 bg-slate-900 flex flex-col z-30 select-none">
        <div className="h-16 flex items-center px-5 border-b border-slate-700/60 shrink-0">
          <div className={accentLogo}>
            <span className="text-white font-extrabold text-sm tracking-tight">IN</span>
          </div>
          <div>
            <p className="text-white font-bold text-base leading-tight tracking-tight">INOUT</p>
            <p className="text-slate-400 text-[10px] leading-tight">
              {isAdmin ? '본사 관리자 콘솔' : '현장 직원 콘솔'}
            </p>
          </div>
        </div>

        <div className="px-5 pt-5 pb-1">
          <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest">메뉴</span>
        </div>

        <nav className="flex-1 px-3 py-1 space-y-0.5 overflow-y-auto">
          {activeNavItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                  isActive
                    ? (isAdmin ? 'bg-indigo-600 text-white shadow-md' : 'bg-emerald-600 text-white shadow-md')
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              {iconMap[item.path]}
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="px-4 py-4 border-t border-slate-700/60 shrink-0">
          <div className="flex items-center gap-3">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0 ${isAdmin ? 'bg-indigo-500' : 'bg-emerald-500'}`}>
              {initial}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-white truncate">{displayName}</p>
              <p className="text-[10px] text-slate-400 truncate">{role} · {storeName || (isAdmin ? '본사' : '매장')}</p>
            </div>
            <button onClick={handleLogout} title="로그아웃" className="text-slate-400 hover:text-rose-400 transition-colors p-1 rounded">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
              </svg>
            </button>
          </div>
        </div>
      </aside>

      <div className="ml-64 flex flex-col min-h-screen">
        <header className="sticky top-0 z-20 h-16 bg-white/90 backdrop-blur-sm border-b border-slate-200/80 flex items-center justify-between px-6 shrink-0">
          <h1 className="text-base font-semibold text-slate-800">{pageTitle}</h1>
          <div className="flex items-center gap-2 sm:gap-3 flex-wrap justify-end">
            {/* 소속 */}
            <div className="hidden md:flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5">
              <span className="text-[11px] font-semibold text-slate-500">{isAdmin ? '소속' : '지점'}</span>
              <span className="text-sm font-bold text-slate-800">{isAdmin ? '본사' : (storeName || '매장')}</span>
            </div>

            {/* 역할별 메타 */}
            {isAdmin ? (
              <>
                {totalDeposit != null && (
                  <div className="hidden lg:flex items-center gap-1.5 rounded-xl border border-violet-200 bg-violet-50 px-3 py-1.5">
                    <span className="text-[11px] font-semibold text-violet-600">전사 예치금</span>
                    <span className="text-sm font-extrabold text-violet-700 tabular-nums">{formatWon(totalDeposit)}</span>
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => navigate('/admin/inquiries')}
                  className="hidden sm:flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 py-1.5 hover:bg-indigo-100 transition-colors"
                >
                  <span className="text-[11px] font-semibold text-indigo-600">미처리 문의</span>
                  <span className="text-sm font-extrabold text-indigo-700 tabular-nums">{unreadInquiryCount}건</span>
                </button>
                <span className="text-[11px] font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100 px-2 py-0.5 rounded-full">
                  ADMIN
                </span>
              </>
            ) : (
              <>
                <button
                  type="button"
                  onClick={() => navigate('/emp/leaves')}
                  className="hidden sm:flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-1.5 hover:bg-emerald-100 transition-colors"
                  title="연차 관리"
                >
                  <span className="text-[11px] font-semibold text-emerald-600">잔여 연차</span>
                  <span className="text-sm font-extrabold text-emerald-700 tabular-nums">
                    {remainingLeaveDays == null ? '-' : `${remainingLeaveDays}일`}
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/emp/deposit')}
                  className="hidden sm:flex items-center gap-1.5 rounded-xl border border-violet-200 bg-violet-50 px-3 py-1.5 hover:bg-violet-100 transition-colors"
                  title="매장 예치금 내역"
                >
                  <span className="text-[11px] font-semibold text-violet-600">예치금 잔액</span>
                  <span className="text-sm font-extrabold text-violet-700 tabular-nums">
                    {empDepositBalance == null ? '-' : formatWon(empDepositBalance)}
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/emp/cart')}
                  className="relative p-2 text-slate-400 hover:text-emerald-600 transition-colors"
                  title="장바구니"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.45L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
                  </svg>
                  {cartCount > 0 && (
                    <span className="absolute top-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white ring-2 ring-white">
                      {cartCount > 9 ? '9+' : cartCount}
                    </span>
                  )}
                </button>
                <span className="text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-100 px-2 py-0.5 rounded-full">
                  EMPLOYEE
                </span>
              </>
            )}

            <div className="flex items-center gap-2 border-r border-slate-200 pr-3">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold ${isAdmin ? 'bg-indigo-500' : 'bg-emerald-500'}`}>
                {initial}
              </div>
              <span className="hidden sm:block text-sm text-slate-600 truncate max-w-[140px]">{displayName}</span>
            </div>

            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-white border border-rose-200 text-rose-600 hover:bg-rose-50 hover:border-rose-300 transition-all active:scale-[0.98] shadow-sm"
            >
              로그아웃
            </button>
          </div>
        </header>

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
