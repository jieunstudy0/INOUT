import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOwnerDashboardSummary } from '../../api/dashboardOwnerApi';
import { getOwnerOrders } from '../../api/orderOwnerApi';
import { getVacationList } from '../../api/vacationOwnerApi';
import { requestOwnerCharge } from '../../api/depositOwnerApi';
import { ForbiddenNotice } from '../../components/auth/RoleGuard';
import { Toast } from '../../utils/toast';
import Spinner from '../../components/common/Spinner';
import DashboardShell from '../../components/dashboard/DashboardShell';
import KpiCard from '../../components/dashboard/KpiCard';
import DashboardPanel, { ProgressBar } from '../../components/dashboard/DashboardPanel';
import RecentActivityTable from '../../components/dashboard/RecentActivityTable';
import StatusBadge from '../../components/dashboard/StatusBadge';
import { dispatchHeaderRefresh } from '../../utils/headerSync';

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
          className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-400/40 focus:border-indigo-400"
        />
        <div className="flex gap-2 mt-3">
          {[100000, 500000, 1000000].map((v) => (
            <button
              key={v}
              type="button"
              onClick={() => setAmount((raw + v).toLocaleString('ko-KR'))}
              className="flex-1 py-2 text-xs font-semibold rounded-lg bg-slate-100 text-slate-600 hover:bg-slate-200"
            >
              +{(v / 10000).toLocaleString()}만
            </button>
          ))}
        </div>
        <div className="flex gap-2 mt-5">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl">취소</button>
          <button type="button" onClick={submit} disabled={submitting} className="flex-1 py-2.5 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50">
            {submitting ? '신청 중...' : '충전 신청'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function OwnerDashboard() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [pendingLeaves, setPendingLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [chargeOpen, setChargeOpen] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getOwnerDashboardSummary(),
      getOwnerOrders({ page: 0, size: 8 }),
      getVacationList({ status: 'PENDING', page: 0, size: 5 }),
    ])
      .then(([summary, ordersPage, leavesPage]) => {
        setData(summary);
        setRecentOrders(ordersPage?.content || []);
        setPendingLeaves(leavesPage?.content || []);
      })
      .catch(() => Toast.error('매장 대시보드를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const ready = data?.readyDeliveryCount ?? 0;
  const shipping = data?.shippingDeliveryCount ?? 0;
  const done = data?.completedDeliveryCount ?? 0;
  const deliveryTotal = ready + shipping + done;
  const todayOrders = data?.todayOrderCount ?? 0;

  return (
    <>
      <ForbiddenNotice />
      <DashboardShell
        title="매장 대시보드"
        subtitle={data ? (
          <>
            <span className="font-semibold text-slate-700">{data.storeName}</span>
            {' · '}안녕하세요, <span className="font-semibold">{data.ownerName}</span> 점주님
          </>
        ) : '우리 매장 발주 · 배송 · 예치금'}
        actions={(
          <button onClick={load} className="px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm">
            새로고침
          </button>
        )}
        kpiSlot={(
          <>
            <KpiCard
              title="매장 예치금"
              value={`${Number(data?.depositBalance ?? 0).toLocaleString('ko-KR')}원`}
              sub="매장 단위 지갑 잔액"
              accent="emerald"
              loading={loading}
              href="/owner/deposit"
              icon={(
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              )}
              action={(
                <button
                  type="button"
                  onClick={() => setChargeOpen(true)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700"
                >
                  충전 신청
                </button>
              )}
            />
            <KpiCard
              title="오늘 발주"
              value={`${todayOrders}건`}
              sub={`배송중 ${shipping} · 완료 ${done}`}
              accent="sky"
              loading={loading}
              href="/owner/orders"
              icon={(
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
                </svg>
              )}
            />
            <KpiCard
              title="승인 대기 연차"
              value={`${data?.pendingLeaveCount ?? 0}건`}
              sub="직원 연차 승인 대기"
              accent="amber"
              loading={loading}
              href="/owner/leaves"
              icon={(
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                </svg>
              )}
            />
            <KpiCard
              title="우리 매장 직원"
              value={`${data?.staffCount ?? 0}명`}
              sub={`잠긴 계정 ${data?.lockedStaffCount ?? 0}명`}
              accent="indigo"
              loading={loading}
              href="/owner/users"
              icon={(
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0z" />
                </svg>
              )}
            />
          </>
        )}
        mainSlot={(
          <>
            <DashboardPanel
              title="오늘 발주 / 배송 현황"
              subtitle="우리 매장 기준"
              className="lg:col-span-2"
              action={(
                <button onClick={() => navigate('/owner/orders')} className="text-xs font-semibold text-indigo-600 hover:underline">
                  발주/배송 보기
                </button>
              )}
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-3">오늘 발주</p>
                  <p className="text-4xl font-extrabold text-slate-800 tabular-nums">{loading ? '—' : todayOrders}</p>
                  <p className="text-xs text-slate-400 mt-1">건 접수</p>
                </div>
                <div className="space-y-3">
                  <ProgressBar label="배송 준비" value={ready} total={deliveryTotal || 1} colorClass="bg-slate-300" textClass="text-slate-600" />
                  <ProgressBar label="배송 중" value={shipping} total={deliveryTotal || 1} colorClass="bg-sky-400" textClass="text-sky-700" />
                  <ProgressBar label="배송 완료" value={done} total={deliveryTotal || 1} colorClass="bg-emerald-400" textClass="text-emerald-700" />
                </div>
              </div>
            </DashboardPanel>

            <DashboardPanel
              title="예치금 · 긴급 알림"
              subtitle="충전 및 미처리 연차"
            >
              <div className="space-y-4">
                <div className="rounded-xl bg-indigo-50 border border-indigo-100 p-4">
                  <p className="text-xs font-semibold text-indigo-600">현재 잔액</p>
                  <p className="text-2xl font-extrabold text-indigo-800 tabular-nums mt-1">
                    {loading ? '—' : `${Number(data?.depositBalance ?? 0).toLocaleString('ko-KR')}원`}
                  </p>
                  <button
                    type="button"
                    onClick={() => setChargeOpen(true)}
                    className="mt-3 w-full py-2 text-xs font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700"
                  >
                    예치금 충전 신청
                  </button>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-500 mb-2">미처리 연차</p>
                  {loading ? (
                    <div className="py-4 flex justify-center"><Spinner size="sm" /></div>
                  ) : pendingLeaves.length === 0 ? (
                    <p className="text-sm text-slate-400 py-3 text-center">대기 중인 연차가 없습니다.</p>
                  ) : (
                    <ul className="space-y-2">
                      {pendingLeaves.slice(0, 3).map((leave) => (
                        <li key={leave.leaveId} className="flex items-center justify-between gap-2 text-sm">
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-800 truncate">{leave.employeeName}</p>
                            <p className="text-[11px] text-slate-400">{leave.startDate} ~ {leave.endDate}</p>
                          </div>
                          <StatusBadge status="PENDING" />
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </DashboardPanel>
          </>
        )}
        activitySlot={(
          <RecentActivityTable
            title="최근 발주 내역"
            subtitle="매장 직원 발주"
            loading={loading}
            emptyMessage="최근 발주가 없습니다."
            action={(
              <button onClick={() => navigate('/owner/orders')} className="text-xs font-semibold text-indigo-600 hover:underline">
                전체 보기
              </button>
            )}
            columns={[
              {
                key: 'order',
                label: '주문',
                render: (o) => (
                  <div>
                    <p className="font-semibold text-slate-800">#{o.orderRequestId}</p>
                    <p className="text-xs text-slate-400">{o.representativeItemName}</p>
                  </div>
                ),
              },
              { key: 'employeeName', label: '신청자', render: (o) => <span className="text-slate-600">{o.employeeName}</span> },
              { key: 'status', label: '상태', render: (o) => <StatusBadge status={o.status} /> },
              {
                key: 'totalPrice',
                label: '금액',
                align: 'right',
                render: (o) => (
                  <span className="font-bold text-slate-800 tabular-nums">
                    {Number(o.totalPrice || 0).toLocaleString('ko-KR')}원
                  </span>
                ),
              },
            ]}
            rows={recentOrders}
          />
        )}
      />
      <ChargeModal
        open={chargeOpen}
        onClose={() => setChargeOpen(false)}
        onSuccess={() => {
          load();
          dispatchHeaderRefresh({ role: 'OWNER' });
        }}
      />
    </>
  );
}
