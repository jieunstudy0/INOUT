import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEmpDashboardSummary } from '../api/dashboardEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

function KpiCard({ title, value, sub, icon, accentClass, href, loading }) {
  const navigate = useNavigate();
  return (
    <div
      className={`relative bg-white rounded-2xl border border-slate-200 shadow-sm p-5 overflow-hidden group transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 ${href ? 'cursor-pointer' : ''}`}
      onClick={href ? () => navigate(href) : undefined}
    >
      <div className={`absolute -right-3 -top-3 w-20 h-20 rounded-full opacity-10 ${accentClass}`} />
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</p>
          {loading ? <div className="mt-3"><Spinner size="sm" /></div> : (
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

function ProgressBar({ label, value, total, colorClass, textClass }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-sm">
        <span className={`font-medium ${textClass || 'text-slate-600'}`}>{label}</span>
        <span className="text-slate-400 text-xs tabular-nums">
          {value.toLocaleString()}건 <span className="text-slate-300 mx-1">·</span>
          <span className={`font-semibold ${textClass || ''}`}>{pct}%</span>
        </span>
      </div>
      <div className="h-2.5 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-700 ${colorClass}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

/* ── 직원용 재고 패널 ── */
function StockEmpPanel({ data, loading }) {
  const total  = data?.totalActiveStockCount ?? 0;
  const normal = data?.normalStockCount      ?? 0;
  const low    = data ? (data.lowStockCount - data.outOfStockCount) : 0;
  const out    = data?.outOfStockCount       ?? 0;
  const navigate = useNavigate();

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-sm font-bold text-slate-800">우리 매장 재고 상태</h3>
          <p className="text-xs text-slate-400 mt-0.5">본사 등록 품목 기준</p>
        </div>
        <button onClick={() => navigate('/emp/stocks')} className="text-xs font-medium text-indigo-500 hover:text-indigo-700 flex items-center gap-1 transition-colors">
          재고 조회 <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" /></svg>
        </button>
      </div>
      {loading ? <div className="flex-1 flex justify-center py-8"><Spinner /></div> : (
        <div className="space-y-4">
          <ProgressBar label="주문 가능" value={normal} total={total} colorClass="bg-emerald-400" textClass="text-emerald-700" />
          <ProgressBar label="품절 임박" value={low}    total={total} colorClass="bg-amber-400"  textClass="text-amber-700" />
          <ProgressBar label="본사 품절" value={out}    total={total} colorClass="bg-rose-400"   textClass="text-rose-700" />
        </div>
      )}
    </div>
  );
}

/* ── 직원용 발주 현황 패널 ── */
function OrderEmpPanel({ data, loading }) {
  const total      = data?.totalOrderCount      ?? 0;
  const completed  = data?.completedOrderCount ?? 0;
  const rejected   = data?.rejectedOrderCount  ?? 0;
  const inProgress = Math.max(total - completed - rejected, 0);
  const navigate   = useNavigate();

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-sm font-bold text-slate-800">내 발주 진행 현황</h3>
          <p className="text-xs text-slate-400 mt-0.5">총 {total.toLocaleString()}건 신청</p>
        </div>
        <button onClick={() => navigate('/emp/orders')} className="text-xs font-medium text-indigo-500 hover:text-indigo-700 flex items-center gap-1 transition-colors">
          발주 내역 <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" /></svg>
        </button>
      </div>
      {loading ? <div className="flex-1 flex justify-center py-8"><Spinner /></div> : (
        <div className="space-y-4">
          <ProgressBar label="진행 중 (승인/배송)" value={inProgress} total={total} colorClass="bg-sky-400" textClass="text-sky-600" />
          <ProgressBar label="처리 완료"           value={completed}  total={total} colorClass="bg-indigo-500" textClass="text-indigo-600" />
          <ProgressBar label="반려됨"              value={rejected}   total={total} colorClass="bg-rose-400" textClass="text-rose-600" />
        </div>
      )}
    </div>
  );
}

const FEED_ICONS = {
  ORDER_APPROVED: { bg: 'bg-emerald-50', ring: 'ring-emerald-100', color: 'text-emerald-500'}, 
  ORDER_REJECTED: { bg: 'bg-rose-50',    ring: 'ring-rose-100',    color: 'text-rose-500'   },
  ORDER_PARTIAL:  { bg: 'bg-amber-50',   ring: 'ring-amber-100',   color: 'text-amber-500'  }, // 💡 이 줄 추가!
  STOCK_USED:     { bg: 'bg-amber-50',   ring: 'ring-amber-100',   color: 'text-amber-500'  },
  INQUIRY_REPLY:  { bg: 'bg-blue-50',    ring: 'ring-blue-100',    color: 'text-blue-500'   },
};

function ActivityFeedPanel({ activities, loading }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col h-full">
      <div className="mb-4 shrink-0">
        <h3 className="text-sm font-bold text-slate-800">내 매장 알림 피드</h3>
        <p className="text-xs text-slate-400 mt-0.5">발주 승인 · 문의 답변 · 재고 알림</p>
      </div>
      <div className="flex-1 overflow-y-auto">
        {loading ? <div className="flex justify-center py-10"><Spinner /></div>
        : !activities?.length ? <p className="text-sm text-center text-slate-400 py-10">최근 알림이 없습니다.</p>
        : (
          <ul className="divide-y divide-slate-50">
            {activities.map((item, idx) => {
              const cfg = FEED_ICONS[item.type] || FEED_ICONS.STOCK_USED;
              return (
                <li key={idx} className="flex items-start gap-3 py-3">
                  <div className={`mt-0.5 w-7 h-7 rounded-full flex items-center justify-center shrink-0 ring-1 ${cfg.bg} ${cfg.ring}`}>
                    <span className={`text-xs ${cfg.color}`}>●</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-slate-700 leading-snug">{item.message}</p>
                  </div>
                  <span className="text-xs text-slate-400 shrink-0 mt-0.5">{item.time}</span>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}

const QUICK_MENUS = [
  { label: '장바구니', path: '/emp/cart',      desc: '발주 대기 상품', color: 'text-indigo-600 bg-indigo-50 border-indigo-100' },
  { label: '발주 내역', path: '/emp/orders',    desc: '신청 내역 조회', color: 'text-sky-600 bg-sky-50 border-sky-100' },
  { label: '문의 사항', path: '/emp/inquiries', desc: '본사 1:1 소통', color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
  { label: '예치금 내역', path: '/emp/deposit', desc: '잔액 및 결제 이력', color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
];

function QuickMenuBar() {
  const navigate = useNavigate();
  return (
    <div className="grid grid-cols-2 gap-3">
      {QUICK_MENUS.map((m) => (
        <button key={m.path} onClick={() => navigate(m.path)}
          className={`flex items-center gap-3 p-4 rounded-2xl border transition-all duration-150 hover:shadow-md hover:-translate-y-0.5 ${m.color} text-left`}>
          <div className="min-w-0">
            <p className="text-sm font-semibold leading-tight">{m.label}</p>
            <p className="text-[11px] opacity-60 mt-0.5">{m.desc}</p>
          </div>
        </button>
      ))}
    </div>
  );
}

export default function DashboardEmpPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getEmpDashboardSummary()
      .then(setData)
      .catch(() => Toast.error('대시보드를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">매장 업무 대시보드</h2>
          {data && <p className="text-sm text-slate-500 mt-0.5">안녕하세요, <span className="font-semibold">{data.userName}</span>님</p>}
        </div>
      </div>

      {/* 💡 핵심 KPI: 예치금, 장바구니, 진행발주, 재고사용 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard title="나의 예치금 잔액" value={data?.depositBalance ?? 0} sub="결제 가능 금액" href="/emp/deposit" accentClass="bg-emerald-400" loading={loading}
          icon={<svg className="w-5 h-5 text-emerald-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>} />
        <KpiCard title="장바구니 상품" value={`${data?.cartItemCount ?? 0}종`} sub="발주 대기 중" href="/emp/cart" accentClass="bg-indigo-400" loading={loading}
          icon={<svg className="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.45L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" /></svg>} />
        <KpiCard title="진행 중인 발주" value={`${data?.inProgressOrderCount ?? 0}건`} sub="승인 대기 및 배송 중" href="/emp/orders" accentClass="bg-sky-400" loading={loading}
          icon={<svg className="w-5 h-5 text-sky-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" /></svg>} />
        <KpiCard title="금일 재고 사용" value={`${data?.todayStockUseCount ?? 0}회`} sub="오늘 매장에서 차감한 횟수" accentClass="bg-amber-400" loading={loading}
          icon={<svg className="w-5 h-5 text-amber-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5m.75-9l3-3 2.148 2.148A12.061 12.061 0 0116.5 7.605" /></svg>} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <StockEmpPanel data={data} loading={loading} />
            <OrderEmpPanel data={data} loading={loading} />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-800 mb-3">바로가기 메뉴</h3>
            <QuickMenuBar />
          </div>
        </div>
        <div>

          <ActivityFeedPanel activities={data?.recentActivities} loading={loading} />
        </div>
      </div>
    </div>
  );
}