import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEmpDashboardSummary } from '../../api/dashboardEmpApi';
import { getMyOrderHistory } from '../../api/orderEmpApi';
import { getMyLeaveList } from '../../api/vacationEmpApi';
import { Toast } from '../../utils/toast';
import DashboardShell from '../../components/dashboard/DashboardShell';
import KpiCard from '../../components/dashboard/KpiCard';
import DashboardPanel, { ProgressBar } from '../../components/dashboard/DashboardPanel';
import RecentActivityTable from '../../components/dashboard/RecentActivityTable';
import StatusBadge from '../../components/dashboard/StatusBadge';

export default function EmpDashboard() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getEmpDashboardSummary(),
      getMyOrderHistory().catch(() => []),
      getMyLeaveList({ page: 0, size: 5 }).catch(() => ({ content: [] })),
    ])
      .then(([summary, orders, leavePage]) => {
        setData(summary);
        const list = Array.isArray(orders) ? orders : (orders?.content || []);
        setRecentOrders(list.slice(0, 8));
        setLeaves(leavePage?.content || (Array.isArray(leavePage) ? leavePage : []));
      })
      .catch(() => Toast.error('대시보드를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const total = data?.totalOrderCount ?? 0;
  const completed = data?.completedOrderCount ?? 0;
  const rejected = data?.rejectedOrderCount ?? 0;
  const inProgress = Math.max(total - completed - rejected, 0);

  const stockTotal = data?.totalActiveStockCount ?? 0;
  const normal = data?.normalStockCount ?? 0;
  const out = data?.outOfStockCount ?? 0;
  const low = data ? Math.max((data.lowStockCount || 0) - out, 0) : 0;

  const pendingLeaves = leaves.filter((l) => l.status === 'PENDING' || l.leaveStatus === 'PENDING').length;
  const approvedLeaves = leaves.filter((l) => l.status === 'APPROVED' || l.leaveStatus === 'APPROVED').length;

  return (
    <DashboardShell
      title="현장 업무 대시보드"
      subtitle={data ? (
        <>안녕하세요, <span className="font-semibold text-slate-700">{data.userName}</span>님
          {data.storeName && <> · <span className="font-semibold">{data.storeName}</span></>}
        </>
      ) : '내 근태 · 발주 · 재고'}
      actions={(
        <button onClick={load} className="px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm">
          새로고침
        </button>
      )}
      kpiSlot={(
        <>
          <KpiCard
            title="예치금 잔액"
            value={`${Number(data?.depositBalance ?? 0).toLocaleString('ko-KR')}원`}
            sub="결제 가능 금액"
            href="/emp/deposit"
            accent="emerald"
            loading={loading}
            icon={(
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            )}
          />
          <KpiCard
            title="장바구니"
            value={`${data?.cartItemCount ?? 0}종`}
            sub="발주 대기 중"
            href="/emp/cart"
            accent="indigo"
            loading={loading}
            icon={(
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.45L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" />
              </svg>
            )}
          />
          <KpiCard
            title="진행 중 발주"
            value={`${data?.inProgressOrderCount ?? 0}건`}
            sub="승인 대기 · 배송 중"
            href="/emp/orders"
            accent="sky"
            loading={loading}
            icon={(
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" />
              </svg>
            )}
          />
          <KpiCard
            title="연차 / 재고사용"
            value={`대기 ${pendingLeaves} · 사용 ${data?.todayStockUseCount ?? 0}`}
            sub={`승인 ${approvedLeaves}건 · 오늘 재고 차감`}
            accent="amber"
            loading={loading}
            icon={(
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
              </svg>
            )}
          />
        </>
      )}
      mainSlot={(
        <>
          <DashboardPanel title="내 발주 진행" subtitle={`총 ${total.toLocaleString('ko-KR')}건`}>
            <div className="space-y-3">
              <ProgressBar label="진행 중" value={inProgress} total={total || 1} colorClass="bg-sky-400" textClass="text-sky-600" />
              <ProgressBar label="완료" value={completed} total={total || 1} colorClass="bg-indigo-500" textClass="text-indigo-600" />
              <ProgressBar label="반려" value={rejected} total={total || 1} colorClass="bg-rose-400" textClass="text-rose-600" />
            </div>
            <button
              type="button"
              onClick={() => navigate('/emp/cart')}
              className="mt-4 w-full py-2 text-xs font-bold rounded-lg border border-indigo-200 text-indigo-700 hover:bg-indigo-50"
            >
              장바구니에서 스마트 추천 보기
            </button>
          </DashboardPanel>
          <DashboardPanel title="내 근태 / 연차" subtitle="최근 신청 기준">
            {leaves.length === 0 ? (
              <p className="text-sm text-slate-400 py-6 text-center">연차 신청 내역이 없습니다.</p>
            ) : (
              <ul className="space-y-2.5">
                {leaves.slice(0, 4).map((leave) => {
                  const status = leave.status || leave.leaveStatus || 'PENDING';
                  return (
                    <li
                      key={leave.leaveId || leave.id}
                      className="flex items-center justify-between gap-2 cursor-pointer hover:bg-slate-50 rounded-lg px-1 -mx-1"
                      onClick={() => navigate(`/emp/leaves/${leave.leaveId || leave.id}`)}
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-800 truncate">
                          {leave.type || leave.leaveType || '연차'}
                        </p>
                        <p className="text-[11px] text-slate-400">
                          {leave.startDate} ~ {leave.endDate}
                        </p>
                      </div>
                      <StatusBadge status={status} />
                    </li>
                  );
                })}
              </ul>
            )}
            <button
              type="button"
              onClick={() => navigate('/emp/leaves')}
              className="mt-4 w-full py-2 text-xs font-bold rounded-lg border border-emerald-200 text-emerald-700 hover:bg-emerald-50"
            >
              연차 관리 / 신청
            </button>
          </DashboardPanel>
          <DashboardPanel title="매장 재고 상태" subtitle="본사 등록 품목">
            <div className="space-y-3">
              <ProgressBar label="주문 가능" value={normal} total={stockTotal || 1} colorClass="bg-emerald-400" textClass="text-emerald-700" />
              <ProgressBar label="품절 임박" value={low} total={stockTotal || 1} colorClass="bg-amber-400" textClass="text-amber-700" />
              <ProgressBar label="본사 품절" value={out} total={stockTotal || 1} colorClass="bg-rose-400" textClass="text-rose-700" />
            </div>
            <button
              type="button"
              onClick={() => navigate('/emp/stocks')}
              className="mt-4 w-full py-2 text-xs font-bold rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50"
            >
              재고 조회하기
            </button>
          </DashboardPanel>
        </>
      )}
      activitySlot={(
        <RecentActivityTable
          title="내 최근 발주"
          subtitle="신청 · 승인 · 배송 상태"
          loading={loading}
          emptyMessage="발주 내역이 없습니다."
          action={(
            <button onClick={() => navigate('/emp/orders')} className="text-xs font-semibold text-indigo-600 hover:underline">
              전체 보기
            </button>
          )}
          onRowClick={() => navigate('/emp/orders')}
          columns={[
            {
              key: 'order',
              label: '주문',
              render: (o) => (
                <div>
                  <p className="font-semibold text-slate-800">#{o.orderRequestId}</p>
                  <p className="text-xs text-slate-400">
                    {o.requestDate ? new Date(o.requestDate).toLocaleDateString('ko-KR') : '-'}
                  </p>
                </div>
              ),
            },
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
  );
}
