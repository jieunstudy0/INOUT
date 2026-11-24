import { useEffect, useState, useCallback } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import NotificationBell from '../common/NotificationBell';
import InstallAppBanner from '../common/InstallAppBanner';
import TopNav from './TopNav';
import SideNav from './SideNav';
import { OWNER_MENU } from './navMenus';
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

/** 점주 레이아웃: 상단 메뉴 + 가운데 본문 */
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
    setUser({ email, initial: email.charAt(0).toUpperCase() });
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

  const ownerName = summary?.ownerName || user?.email || '점주';
  const storeName = summary?.storeName || '매장';
  const depositBalance = summary?.depositBalance ?? 0;

  const utility = (
    <>
      <span>{storeName}</span>
      <span className="flex items-center gap-2">
        예치금 <b className="font-semibold text-slate-900 tabular-nums">{formatWon(depositBalance)}</b>
        <button type="button" onClick={() => setChargeOpen(true)}
          className="px-2 py-0.5 text-[11px] rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50">
          충전 신청
        </button>
      </span>
      <span className="text-slate-900">{ownerName} 점주님</span>
    </>
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
        menu={OWNER_MENU}
        utility={utility}
        actions={<><NotificationBell basePath="/owner" />{logoutButton}</>}
      />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 sm:py-8 lg:flex lg:gap-10">
        <SideNav menu={OWNER_MENU} />
        <div className="min-w-0 flex-1">
          <InstallAppBanner />
          <Outlet context={{ summary, refreshSummary }} />
        </div>
      </main>

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
