import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOwnerDashboardSummary } from '../api/dashboardOwnerApi';
import { ForbiddenNotice } from '../components/auth/RoleGuard';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

function KpiCard({ title, value, sub, href, accentClass, loading, icon }) {
  const navigate = useNavigate();
  return (
    <div
      className={`relative bg-white rounded-2xl border border-slate-200 shadow-sm p-5 overflow-hidden transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 ${href ? 'cursor-pointer' : ''}`}
      onClick={href ? () => navigate(href) : undefined}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</p>
          {loading ? (
            <div className="mt-3"><Spinner size="sm" /></div>
          ) : (
            <p className="mt-2 text-3xl font-extrabold text-slate-800 tabular-nums leading-none">
              {typeof value === 'number' ? value.toLocaleString('ko-KR') : value}
            </p>
          )}
          {sub && !loading && <p className="mt-1.5 text-xs text-slate-400">{sub}</p>}
        </div>
        <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${accentClass} bg-opacity-15`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export default function DashboardOwnerPage() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    getOwnerDashboardSummary()
      .then(setData)
      .catch(() => Toast.error('매장 대시보드를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <ForbiddenNotice />
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">매장 대시보드</h2>
          {data && (
            <p className="text-sm text-slate-500 mt-0.5">
              <span className="font-semibold text-slate-700">{data.storeName}</span>
              {' · '}안녕하세요, <span className="font-semibold">{data.ownerName}</span> 점주님
            </p>
          )}
        </div>
        <button
          onClick={load}
          className="px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm"
        >
          새로고침
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard
          title="오늘 발주"
          value={`${data?.todayOrderCount ?? 0}건`}
          sub="금일 매장 발주 신청"
          href="/owner/orders"
          accentClass="bg-sky-400"
          loading={loading}
          icon={<svg className="w-5 h-5 text-sky-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" /></svg>}
        />
        <KpiCard
          title="배송 중"
          value={`${data?.shippingDeliveryCount ?? 0}건`}
          sub={`준비 ${data?.readyDeliveryCount ?? 0} · 완료 ${data?.completedDeliveryCount ?? 0}`}
          href="/owner/delivery"
          accentClass="bg-indigo-400"
          loading={loading}
          icon={<svg className="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" /></svg>}
        />
        <KpiCard
          title="연차 대기"
          value={`${data?.pendingLeaveCount ?? 0}건`}
          sub="승인 대기 중인 직원 연차"
          href="/owner/vacation"
          accentClass="bg-amber-400"
          loading={loading}
          icon={<svg className="w-5 h-5 text-amber-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" /></svg>}
        />
        <KpiCard
          title="매장 예치금"
          value={data?.depositBalance ?? 0}
          sub={`승인 대기 발주 ${data?.pendingOrderCount ?? 0}건`}
          href="/owner/deposit"
          accentClass="bg-emerald-400"
          loading={loading}
          icon={<svg className="w-5 h-5 text-emerald-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          onClick={() => navigate('/owner/users')}
          className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 text-left hover:shadow-md transition-all"
        >
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">직원 관리</p>
          <p className="mt-2 text-2xl font-extrabold text-slate-800">
            {loading ? '—' : `${data?.staffCount ?? 0}명`}
          </p>
          <p className="mt-1 text-xs text-slate-400">
            잠긴 계정 {data?.lockedStaffCount ?? 0}명 · 퇴사/잠금해제/비밀번호 초기화
          </p>
        </button>
        <button
          onClick={() => navigate('/owner/stocks')}
          className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 text-left hover:shadow-md transition-all"
        >
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">본사 재고 · 발주</p>
          <p className="mt-2 text-sm font-semibold text-slate-700">재고 조회 후 장바구니로 발주 신청</p>
          <p className="mt-1 text-xs text-slate-400">최종 승인·재고 수량 수정은 본사(ADMIN) 전용</p>
        </button>
      </div>
    </div>
  );
}
