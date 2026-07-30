import { useEffect, useState, useCallback } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { logout } from '../../api/authApi';
import { getOwnerDashboardSummary } from '../../api/dashboardOwnerApi';
import { requestOwnerCharge } from '../../api/depositOwnerApi';
import { Toast } from '../../utils/toast';
import {
  dispatchHeaderRefresh,
  subscribeHeaderRefresh,
  formatWon,
  parseJwtPayload,
} from '../../utils/headerSync';

const OWNER_NAV_ITEMS = [
  { path: '/owner/dashboard', label: '대시보드' },
  { path: '/owner/users', label: '직원 관리' },
  { path: '/owner/orders', label: '발주/배송 조회' },
  { path: '/owner/deposit', label: '예치금 관리' },
  { path: '/owner/leaves', label: '연차 관리' },
];

const PAGE_TITLE_MAP = {
  '/owner/dashboard': '매장 대시보드',
  '/owner/users': '직원 관리',
  '/owner/deposit': '예치금 관리',
  '/owner/orders': '발주/배송 조회',
  '/owner/leaves': '연차 관리',
};

const NAV_ICONS = {
  '/owner/dashboard': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
    </svg>
  ),
  '/owner/users': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
    </svg>
  ),
  '/owner/orders': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
    </svg>
  ),
  '/owner/deposit': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a2.25 2.25 0 00-2.25-2.25H15a3 3 0 11-6 0H5.25A2.25 2.25 0 003 12m18 0v6a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18v-6m18 0V9M3 12V9m18 0a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 9m18 0V6a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 6v3" />
    </svg>
  ),
  '/owner/leaves': (
    <svg className="w-[18px] h-[18px] shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
    </svg>
  ),
};

function ChargeModal({ open, onClose, onSuccess }) {
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  if (!open) return null;
  const raw = Number(String(amount).replace(/,/g, '')) || 0;

  const submit = async () => {
    if (raw <= 0) {
      Toast.error('충전 신청 금액을 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await requestOwnerCharge({ amount: raw });
      Toast.success('예치금 충전 신청이 완료되었습니다. 본사 승인 후 반영됩니다.');
      setAmount('');
      onSuccess?.();
      onClose();
    } catch {
      /* interceptor */
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <h3 className="text-lg font-bold text-slate-800 mb-1">예치금 충전 신청</h3>
        <p className="text-sm text-slate-500 mb-4">본사 관리자 승인 후 매장 예치금에 반영됩니다.</p>
        <input
          type="text"
          inputMode="numeric"
          value={amount}
          onChange={(e) => {
            const digits = e.target.value.replace(/[^0-9]/g, '');
            setAmount(digits ? Number(digits).toLocaleString('ko-KR') : '');
          }}
          placeholder="신청 금액"
          className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-400/40 focus:border-sky-400"
        />
        <div className="flex gap-2 mt-5">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl">취소</button>
          <button type="button" onClick={submit} disabled={submitting} className="flex-1 py-2.5 text-sm font-bold bg-sky-600 text-white rounded-xl hover:bg-sky-700 disabled:opacity-50">
            {submitting ? '신청 중...' : '충전 신청'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function OwnerLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState(null);
  const [summary, setSummary] = useState(null);
  const [chargeOpen, setChargeOpen] = useState(false);

  const loadSummary = useCallback(() => {
    getOwnerDashboardSummary()
      .then(setSummary)
      .catch(() => setSummary(null));
  }, []);

  /** Outlet 자식에 넘기는 콜백 — 매 렌더마다 새 함수가 되면 예치금 페이지 load 의존성이 깨져 무한 로딩됨 */
  const refreshSummary = useCallback(() => {
    loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    const payload = parseJwtPayload();
    if (!payload) {
      navigate('/login', { replace: true });
      return;
    }
    const email = payload.sub || payload.email || '점주';
    setUser({
      email,
      initial: email.charAt(0).toUpperCase(),
    });
    loadSummary();
  }, [navigate, loadSummary, location.pathname]);

  useEffect(() => subscribeHeaderRefresh(() => loadSummary()), [loadSummary]);

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

  const pageTitle = PAGE_TITLE_MAP[location.pathname] || '가맹점주';
  const ownerName = summary?.ownerName || user?.email || '점주';
  const storeName = summary?.storeName || '매장';
  const depositBalance = summary?.depositBalance ?? 0;

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 w-64 bg-slate-900 flex flex-col z-30 select-none">
        <div className="h-16 flex items-center px-5 border-b border-slate-700/60 shrink-0">
          <div className="w-8 h-8 bg-sky-500 rounded-lg flex items-center justify-center mr-3 shadow-lg shadow-sky-900/40">
            <span className="text-white font-extrabold text-sm tracking-tight">IN</span>
          </div>
          <div>
            <p className="text-white font-bold text-base leading-tight tracking-tight">INOUT</p>
            <p className="text-slate-400 text-[10px] leading-tight">가맹점주 콘솔</p>
          </div>
        </div>

        <div className="px-5 pt-5 pb-1">
          <span className="text-[10px] font-semibold text-slate-500 uppercase tracking-widest">메뉴</span>
        </div>

        <nav className="flex-1 px-3 py-1 space-y-0.5 overflow-y-auto">
          {OWNER_NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-md'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              {NAV_ICONS[item.path]}
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="px-4 py-4 border-t border-slate-700/60 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-sky-500 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0">
              {(ownerName || '?').charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-white truncate">{ownerName}</p>
              <p className="text-[10px] text-slate-400 truncate">OWNER · {storeName}</p>
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
            <div className="hidden md:flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5">
              <span className="text-[11px] font-semibold text-slate-500">지점</span>
              <span className="text-sm font-bold text-slate-800">{storeName}</span>
            </div>

            <div className="hidden sm:flex items-center gap-1.5 rounded-xl border border-sky-200 bg-sky-50 px-2.5 py-1.5">
              <span className="text-[11px] font-semibold text-sky-600">예치금</span>
              <span className="text-sm font-extrabold text-sky-700 tabular-nums">{formatWon(depositBalance)}</span>
              <button
                type="button"
                onClick={() => setChargeOpen(true)}
                className="ml-1 px-2 py-0.5 text-[10px] font-bold rounded-md bg-sky-600 text-white hover:bg-sky-700"
              >
                충전
              </button>
            </div>

            <span className="text-[11px] font-semibold bg-sky-50 text-sky-700 border border-sky-100 px-2 py-0.5 rounded-full">
              OWNER
            </span>

            <div className="flex items-center gap-2 border-r border-slate-200 pr-3">
              <div className="w-7 h-7 bg-sky-500 rounded-full flex items-center justify-center text-white text-xs font-bold">
                {(ownerName || '?').charAt(0)}
              </div>
              <span className="hidden lg:block text-sm text-slate-600 truncate max-w-[140px]">{ownerName}</span>
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
          <Outlet context={{ summary, refreshSummary }} />
        </main>
      </div>

      <ChargeModal
        open={chargeOpen}
        onClose={() => setChargeOpen(false)}
        onSuccess={() => {
          refreshSummary();
          dispatchHeaderRefresh({ role: 'OWNER' });
        }}
      />
    </div>
  );
}
