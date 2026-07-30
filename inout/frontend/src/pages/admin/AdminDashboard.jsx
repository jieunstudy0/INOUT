import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboardSummary } from '../../api/dashboardApi';
import { getListByStatuses } from '../../api/orderAdmApi';
import { Toast } from '../../utils/toast';
import DashboardShell from '../../components/dashboard/DashboardShell';
import KpiCard from '../../components/dashboard/KpiCard';
import DashboardPanel, { ProgressBar } from '../../components/dashboard/DashboardPanel';
import RecentActivityTable from '../../components/dashboard/RecentActivityTable';
import StatusBadge from '../../components/dashboard/StatusBadge';

function formatCurrency(val) {
  if (val == null) return '-';
  return `${Number(val).toLocaleString('ko-KR')}원`;
}

function formatManwon(val) {
  if (val == null) return '-';
  const man = Math.round(Number(val) / 10000);
  return `${man.toLocaleString('ko-KR')}만원`;
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLast] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getDashboardSummary(),
      getListByStatuses(['PAID', 'PARTIAL', 'COMPLETED', 'REJECTED']).catch(() => []),
    ])
      .then(([summary, orders]) => {
        setData(summary);
        setRecentOrders((Array.isArray(orders) ? orders : []).slice(0, 8));
        setLast(new Date());
      })
      .catch(() => Toast.error('대시보드 데이터를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, 5 * 60 * 1000);
    return () => clearInterval(timer);
  }, [load]);

  const lastFmt = lastUpdated
    ? lastUpdated.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '-';

  const totalStock = data?.totalActiveStockCount ?? 0;
  const normal = data?.normalStockCount ?? 0;
  const out = data?.outOfStockCount ?? 0;
  const low = data ? Math.max((data.lowStockCount || 0) - out, 0) : 0;

  const orderTotal = data?.totalOrderCount ?? 0;
  const completed = data?.completedOrderCount ?? 0;
  const rejected = data?.rejectedOrderCount ?? 0;
  const pending = data?.pendingOrderCount ?? 0;
  const inProgress = Math.max(orderTotal - completed - rejected - pending, 0);

  const deliveryReady = data?.pendingDeliveryCount ?? 0;
  const deliveryShipping = data?.shippingDeliveryCount ?? 0;
  const deliveryDone = data?.completedDeliveryCount ?? 0;
  const deliveryTotal = deliveryReady + deliveryShipping + deliveryDone;

  return (
    <DashboardShell
      title="본사 운영 대시보드"
      subtitle={data ? (
        <>안녕하세요, <span className="font-semibold text-slate-700">{data.userName}</span>님 · 전사 현황</>
      ) : '전사 발주 · 배송 · 재고 현황'}
      actions={(
        <>
          {lastUpdated && <span className="text-xs text-slate-400 hidden sm:block">갱신 {lastFmt}</span>}
          <button
            onClick={load}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm"
          >
            새로고침
          </button>
        </>
      )}
      kpiSlot={(
        <>
          <KpiCard
            title="금일 신규 발주"
            value={`${data?.todayNewOrderCount ?? 0}건`}
            sub="오늘 접수된 발주"
            href="/admin/orders"
            accent="indigo"
            loading={loading}
            icon={<span className="text-lg">📦</span>}
          />
          <KpiCard
            title="금일 주문액"
            value={data ? formatManwon(data.todayOrderAmount) : '-'}
            sub={data ? formatCurrency(data.todayOrderAmount) : '결제·승인 합산'}
            accent="emerald"
            loading={loading}
            icon={<span className="text-lg">💰</span>}
          />
          <KpiCard
            title="AI 발주 대기"
            value={`${data?.aiSuggestedPendingOrderCount ?? 0}건`}
            sub="제안 품목 승인 대기"
            href="/admin/orders"
            accent="teal"
            loading={loading}
            icon={<span className="text-lg">✨</span>}
          />
          <KpiCard
            title="AI CS 초안"
            value={`${data?.aiDraftCompletedCount ?? 0}건`}
            sub={`답변대기 ${data?.waitingCsInquiryCount ?? 0} · 미읽음 ${data?.unreadInquiryCount ?? 0}`}
            href="/admin/inquiries"
            accent="violet"
            loading={loading}
            icon={<span className="text-lg">🤖</span>}
          />
        </>
      )}
      mainSlot={(
        <>
          <DashboardPanel
            title="오늘 발주/배송 · 재고"
            subtitle={`입고 ${data?.todayInCount ?? 0} · 출고 ${data?.todayOutCount ?? 0}`}
            className="lg:col-span-3"
          >
            <div className="space-y-5">
              <div>
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">발주</p>
                <div className="space-y-3">
                  <ProgressBar label="승인 완료" value={completed} total={orderTotal} colorClass="bg-indigo-500" textClass="text-indigo-600" />
                  <ProgressBar label="처리 중" value={inProgress} total={orderTotal} colorClass="bg-sky-400" textClass="text-sky-600" />
                  <ProgressBar label="반려" value={rejected} total={orderTotal} colorClass="bg-rose-400" textClass="text-rose-600" />
                </div>
              </div>
              <div className="border-t border-slate-100 pt-4">
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">배송</p>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { label: '준비', v: deliveryReady, cls: 'bg-slate-50 text-slate-700' },
                    { label: '배송중', v: deliveryShipping, cls: 'bg-sky-50 text-sky-700' },
                    { label: '완료', v: deliveryDone, cls: 'bg-emerald-50 text-emerald-700' },
                  ].map((x) => (
                    <div key={x.label} className={`rounded-xl px-2 py-2.5 text-center ${x.cls}`}>
                      <p className="text-lg font-bold tabular-nums">{x.v}</p>
                      <p className="text-[10px] font-semibold opacity-70">{x.label}</p>
                    </div>
                  ))}
                </div>
                <p className="text-[11px] text-slate-400 mt-2 text-right">
                  완료율 {deliveryTotal > 0 ? Math.round((deliveryDone / deliveryTotal) * 100) : 0}%
                </p>
              </div>
              <div className="border-t border-slate-100 pt-4">
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">재고</p>
                <div className="space-y-3">
                  <ProgressBar label="정상" value={normal} total={totalStock} colorClass="bg-emerald-400" textClass="text-emerald-700" />
                  <ProgressBar label="저재고" value={low} total={totalStock} colorClass="bg-amber-400" textClass="text-amber-700" />
                  <ProgressBar label="품절" value={out} total={totalStock} colorClass="bg-rose-400" textClass="text-rose-700" />
                </div>
              </div>
            </div>
          </DashboardPanel>
        </>
      )}
      activitySlot={(
        <RecentActivityTable
          title="최근 발주 현황"
          subtitle="승인대기 · 부분 · 완료 · 반려"
          loading={loading}
          emptyMessage="최근 발주 데이터가 없습니다."
          action={(
            <button onClick={() => navigate('/admin/orders')} className="text-xs font-semibold text-indigo-600 hover:underline">
              전체 보기
            </button>
          )}
          onRowClick={(row) => navigate('/admin/orders')}
          columns={[
            {
              key: 'order',
              label: '주문',
              render: (o) => (
                <div>
                  <p className="font-semibold text-slate-800">#{o.orderRequestId}</p>
                  <p className="text-xs text-slate-400 truncate max-w-[180px]">{o.representativeItemName || o.storeName}</p>
                </div>
              ),
            },
            { key: 'storeName', label: '매장', render: (o) => <span className="text-slate-600">{o.storeName}</span> },
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
