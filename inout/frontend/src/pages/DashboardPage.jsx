import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboardSummary } from '../api/dashboardApi';
import { logout } from '../api/authApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

function KpiCard({ title, value, sub, icon, accentClass, href, loading }) {
  const navigate = useNavigate();
  return (
    <div
      className={`relative bg-white rounded-2xl border border-slate-200 shadow-sm p-5 overflow-hidden
                  group transition-all duration-200 hover:shadow-md hover:-translate-y-0.5
                  ${href ? 'cursor-pointer' : ''}`}
      onClick={href ? () => navigate(href) : undefined}
    >
      <div className={`absolute -right-3 -top-3 w-20 h-20 rounded-full opacity-10 ${accentClass}`} />
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{title}</p>
          {loading
            ? <div className="mt-3"><Spinner size="sm" /></div>
            : <p className="mt-2 text-2xl font-extrabold text-slate-800 tabular-nums leading-none">
                {typeof value === 'number' ? value.toLocaleString('ko-KR') : value}
              </p>
          }
          {sub && !loading && <p className="mt-1.5 text-xs text-slate-400 truncate">{sub}</p>}
        </div>
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${accentClass} bg-opacity-15`}>
          {icon}
        </div>
      </div>
      {href && (
        <div className="mt-4 flex items-center gap-1 text-xs font-medium text-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity">
          바로가기
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </div>
      )}
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
      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-700 ${colorClass}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function StockDistributionPanel({ data, loading }) {
  const total  = data?.totalActiveStockCount ?? 0;
  const normal = data?.normalStockCount      ?? 0;
  const low    = data ? (data.lowStockCount - data.outOfStockCount) : 0;
  const out    = data?.outOfStockCount       ?? 0;
  const navigate = useNavigate();

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-sm font-bold text-slate-800">재고 상태 분포</h3>
          <p className="text-xs text-slate-400 mt-0.5">전체 {total.toLocaleString()}개 품목</p>
        </div>
        <button onClick={() => navigate('/admin/stock')}
          className="text-xs font-medium text-indigo-500 hover:text-indigo-700 flex items-center gap-1 transition-colors">
          재고 관리
          <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </button>
      </div>
      {loading
        ? <div className="flex-1 flex items-center justify-center py-8"><Spinner /></div>
        : (
          <div className="space-y-4">
            <ProgressBar label="정상"   value={normal} total={total} colorClass="bg-emerald-400" textClass="text-emerald-700" />
            <ProgressBar label="저재고" value={low}    total={total} colorClass="bg-amber-400"   textClass="text-amber-700" />
            <ProgressBar label="품절"   value={out}    total={total} colorClass="bg-rose-400"    textClass="text-rose-700" />
            <div className="pt-2 mt-2 border-t border-slate-100 grid grid-cols-3 gap-2">
              {[
                { label: '정상',   v: normal, cls: 'text-emerald-600 bg-emerald-50' },
                { label: '저재고', v: low,    cls: 'text-amber-600 bg-amber-50'    },
                { label: '품절',   v: out,    cls: 'text-rose-600 bg-rose-50'      },
              ].map((r) => (
                <div key={r.label} className={`rounded-xl px-3 py-2 text-center ${r.cls}`}>
                  <p className="text-lg font-bold tabular-nums">{r.v.toLocaleString()}</p>
                  <p className="text-[10px] font-semibold uppercase tracking-wide opacity-70">{r.label}</p>
                </div>
              ))}
            </div>
          </div>
        )
      }
    </div>
  );
}

function OrderStatusPanel({ data, loading }) {
  const total      = data?.totalOrderCount     ?? 0;
  const completed  = data?.completedOrderCount ?? 0;
  const rejected   = data?.rejectedOrderCount  ?? 0;
  const pending    = data?.pendingOrderCount    ?? 0;
  const inProgress = Math.max(total - completed - rejected - pending, 0);
  const navigate   = useNavigate();

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-sm font-bold text-slate-800">발주 처리 현황</h3>
          <p className="text-xs text-slate-400 mt-0.5">총 {total.toLocaleString()}건 누적</p>
        </div>
        <button onClick={() => navigate('/admin/orders')}
          className="text-xs font-medium text-indigo-500 hover:text-indigo-700 flex items-center gap-1 transition-colors">
          발주 관리
          <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </button>
      </div>
      {loading
        ? <div className="flex-1 flex items-center justify-center py-8"><Spinner /></div>
        : (
          <div className="space-y-4 flex-1 flex flex-col justify-between">
            <div className="space-y-4">
                <ProgressBar label="승인 완료"   value={completed}  total={total} colorClass="bg-indigo-500"  textClass="text-indigo-600" />
                <ProgressBar label="처리 중"     value={inProgress} total={total} colorClass="bg-sky-400"     textClass="text-sky-600" />
                <ProgressBar label="반려"        value={rejected}   total={total} colorClass="bg-rose-400"    textClass="text-rose-600" />
                <ProgressBar label="미결제 대기" value={pending}    total={total} colorClass="bg-slate-300"   textClass="text-slate-500" />
            </div>
            <div className="pt-2 mt-2 border-t border-slate-100 flex items-center gap-3">
              <div className="flex-1 bg-blue-50 rounded-xl px-3 py-2 text-center">
                <p className="text-lg font-bold text-blue-600 tabular-nums">{data?.todayInCount ?? 0}</p>
                <p className="text-[10px] font-semibold text-blue-500 uppercase tracking-wide">금일 입고</p>
              </div>
              <div className="flex-1 bg-orange-50 rounded-xl px-3 py-2 text-center">
                <p className="text-lg font-bold text-orange-600 tabular-nums">{data?.todayOutCount ?? 0}</p>
                <p className="text-[10px] font-semibold text-orange-500 uppercase tracking-wide">금일 출고</p>
              </div>
            </div>
          </div>
        )
      }
    </div>
  );
}

function DeliveryStatusPanel({ data, loading }) {
  const ready     = data?.pendingDeliveryCount ?? 0;
  const shipping  = data?.shippingDeliveryCount ?? 0;
  const completed = data?.completedDeliveryCount ?? 0;
  const total     = ready + shipping + completed;
  const navigate  = useNavigate();

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h3 className="text-sm font-bold text-slate-800">당일 배송 현황</h3>
          <p className="text-xs text-slate-400 mt-0.5">총 {total.toLocaleString()}건 진행/완료</p>
        </div>
        <button onClick={() => navigate('/admin/delivery')}
          className="text-xs font-medium text-indigo-500 hover:text-indigo-700 flex items-center gap-1 transition-colors">
          배송 관리
          <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </button>
      </div>
      {loading
        ? <div className="flex-1 flex items-center justify-center py-8"><Spinner /></div>
        : (
          <div className="space-y-4">
            <ProgressBar label="배송 완료" value={completed} total={total} colorClass="bg-emerald-400" textClass="text-emerald-700" />
            <ProgressBar label="배송 중"   value={shipping}  total={total} colorClass="bg-sky-400"     textClass="text-sky-700" />
            <ProgressBar label="배송 준비" value={ready}     total={total} colorClass="bg-slate-300"   textClass="text-slate-600" />
            
            <div className="mt-4 p-4 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between">
                <div>
                    <p className="text-xs font-semibold text-slate-500">배송 완료율</p>
                    <p className="text-xl font-bold text-slate-800">
                        {total > 0 ? Math.round((completed / total) * 100) : 0}<span className="text-sm text-slate-500 font-medium">%</span>
                    </p>
                </div>
                <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
                    <span className="text-emerald-600 text-lg">📦</span>
                </div>
            </div>
          </div>
        )
      }
    </div>
  );
}

const FEED_ICONS = {
  ORDER_IN:       { bg: 'bg-blue-50',    ring: 'ring-blue-100',    color: 'text-blue-500'   },
  ORDER_REJECTED: { bg: 'bg-rose-50',    ring: 'ring-rose-100',    color: 'text-rose-500'   },
  LOW_STOCK:      { bg: 'bg-amber-50',   ring: 'ring-amber-100',   color: 'text-amber-500'  },
  STOCK_IN:       { bg: 'bg-emerald-50', ring: 'ring-emerald-100', color: 'text-emerald-500'},
};

function ActivityFeedPanel({ activities, loading, onRefresh }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col h-full">
      <div className="flex items-center justify-between mb-4 shrink-0">
        <div>
          <h3 className="text-sm font-bold text-slate-800">실시간 알림 피드</h3>
          <p className="text-xs text-slate-400 mt-0.5">보상 트랜잭션 · 재고 경고 · 발주 이벤트</p>
        </div>
        <button onClick={onRefresh}
          className="text-slate-400 hover:text-indigo-500 transition-colors p-1 rounded-lg hover:bg-slate-100">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
        </button>
      </div>
      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <div className="flex items-center justify-center py-10"><Spinner /></div>
        ) : !activities || activities.length === 0 ? (
          <p className="text-sm text-center text-slate-400 py-10">최근 이벤트가 없습니다.</p>
        ) : (
          <ul className="divide-y divide-slate-50">
            {activities.map((item, idx) => {
              const cfg = FEED_ICONS[item.type] || FEED_ICONS.ORDER_IN;
              return (
                <li key={idx} className="flex items-start gap-3 py-3">
                  <div className={`mt-0.5 w-7 h-7 rounded-full flex items-center justify-center shrink-0 ring-1 ${cfg.bg} ${cfg.ring}`}>
                    <span className={`text-xs ${cfg.color}`}>●</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-slate-700 leading-snug">{item.message}</p>
                  </div>
                  <span className="text-xs text-slate-400 shrink-0 mt-0.5 whitespace-nowrap">{item.time}</span>
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
  { label: '발주 관리', path: '/admin/orders', desc: '승인 대기 발주 처리', color: 'text-indigo-600 bg-indigo-50 border-indigo-100' },
  { label: '재고 관리', path: '/admin/stock',  desc: '입고 처리 · 이력 조회', color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
  { label: '회원 관리', path: '/admin/users',  desc: '사용자 · 권한 관리', color: 'text-violet-600 bg-violet-50 border-violet-100' },
  { 
      label: 'API 문서',  
      path: 'http://localhost:8080/swagger-ui/index.html', 
      desc: 'Swagger OpenAPI 3.0', 
      color: 'text-slate-600 bg-slate-50 border-slate-200', 
      external: true 
  },
];

function QuickMenuBar() {
  const navigate = useNavigate();
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {QUICK_MENUS.map((m) => (
        <button key={m.path}
          onClick={() => m.external ? window.open(m.path, '_blank') : navigate(m.path)}
          className={`flex items-center gap-3 p-4 rounded-2xl border transition-all duration-150 hover:shadow-md hover:-translate-y-0.5 ${m.color} text-left`}>
          <div className="min-w-0">
            <p className="text-sm font-semibold leading-tight">{m.label}</p>
            <p className="text-[11px] opacity-60 truncate mt-0.5">{m.desc}</p>
          </div>
          <svg className="w-4 h-4 ml-auto opacity-40 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </button>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const [data, setData]       = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLast] = useState(null);
  const navigate = useNavigate();

  const load = useCallback(() => {
    setLoading(true);
    getDashboardSummary()
      .then((res) => { setData(res); setLast(new Date()); })
      .catch(() => Toast.error('대시보드 데이터를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, 5 * 60 * 1000);
    return () => clearInterval(timer);
  }, [load]);
  

  const handleLogout = async () => {
    if (!window.confirm('로그아웃 하시겠습니까?')) return;
    try {   
      await logout();
    } catch (err) {
      console.error('로그아웃 요청 실패:', err);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      Toast.success('성공적으로 로그아웃 되었습니다.');
      navigate('/login', { replace: true });
    }
  };

  const formatCurrency = (val) => val != null ? `${(val / 10000).toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}만원` : '-';

  const lastFmt = lastUpdated
    ? lastUpdated.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '-';

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4">
      {/* 페이지 헤더 */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">시스템 현황</h2>
          {data && (
            <div className="flex items-center gap-4 mt-1"> 
              <p className="text-sm text-slate-500">
                안녕하세요, <span className="font-semibold text-slate-700">{data.userName}</span>님
                <span className="mx-1.5 text-slate-300">·</span>관리자 대시보드
              </p>
            </div>
          )}
        </div>
        <div className="flex items-center gap-3">
          {lastUpdated && (
            <span className="text-xs text-slate-400 hidden sm:block">마지막 갱신: {lastFmt}</span>
          )}
          <button onClick={load}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-all shadow-sm">
            <svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
            새로고침
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard title="금일 신규 발주" value={data?.todayNewOrderCount ?? 0} sub="오늘 접수된 발주 건수" href="/admin/orders" accentClass="bg-indigo-400" loading={loading}
          icon={<span className="text-indigo-600 text-lg">📦</span>} />
        <KpiCard title="금일 주문액" value={data ? formatCurrency(data.todayOrderAmount) : '-'} sub="결제완료·승인 주문 합산" accentClass="bg-emerald-400" loading={loading}
          icon={<span className="text-emerald-600 text-lg">💰</span>} />
        <KpiCard title="배송 중" value={data?.shippingDeliveryCount ?? 0} sub="현재 이동 중인 화물" href="/admin/delivery" accentClass="bg-sky-400" loading={loading}
          icon={<span className="text-sky-600 text-lg">🚚</span>} />
        <KpiCard title="배송 완료" value={data?.completedDeliveryCount ?? 0} sub="금일 도착 완료" href="/admin/delivery" accentClass="bg-emerald-400" loading={loading}
          icon={<span className="text-emerald-600 text-lg">✅</span>} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <StockDistributionPanel data={data} loading={loading} />
        <OrderStatusPanel data={data} loading={loading} />
        <DeliveryStatusPanel data={data} loading={loading} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2 space-y-4">
          <div>
            <h3 className="text-sm font-bold text-slate-800 mb-3">바로가기</h3>
            <QuickMenuBar />
          </div>
          {data?.unreadInquiryCount > 0 && (
            <div className="flex items-center gap-3 bg-violet-50 border border-violet-200 rounded-2xl px-5 py-3.5 mt-2">
              <svg className="w-5 h-5 text-violet-500 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
              </svg>
              <p className="text-sm text-violet-700">
                미읽음 문의가 <strong>{data.unreadInquiryCount}건</strong> 있습니다.
              </p>
            </div>
          )}
        </div>
        
        <ActivityFeedPanel activities={data?.recentActivities} loading={loading} onRefresh={load} />
      </div>
    </div>
  );
}