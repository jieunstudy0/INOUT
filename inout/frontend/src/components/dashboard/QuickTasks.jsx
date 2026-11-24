import { useEffect, useState } from 'react';
import QuickTaskCard from './QuickTaskCard';
import { getPendingReceipts, getStoreStocks } from '../../api/storeStockApi';
import { getStockCountSheets, getOwnerStockCountSheets } from '../../api/stockCountApi';
import { getMyChangeRequests, getChangeRequestInbox } from '../../api/changeRequestApi';
import { getLowStockAlerts } from '../../api/stockApi';
import { getListByStatuses } from '../../api/orderAdmApi';
import { getDeliveryList } from '../../api/deliveryApi';

const svg = (d) => (
  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d={d} /></svg>
);
const ICON = {
  truck: svg('M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12'),
  receipt: svg('M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08M15.75 18H8.25a2.25 2.25 0 01-2.25-2.25V6.108c0-1.135.845-2.098 1.976-2.192a48.424 48.424 0 011.123-.08'),
  box: svg('M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z'),
  edit: svg('M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125'),
  check: svg('M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z'),
  order: svg('M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272'),
};

const settle = (p, fallback) => p.then((v) => v).catch(() => fallback);
const isOpen = (r) => r.status === 'REQUESTED' || r.status === 'IN_PROGRESS';

/** 역할별로 지금 처리할 일을 모아 숫자로 보여 준다 (기존 API를 그대로 불러 계산). role: 'emp' | 'owner' | 'admin' */
async function loadTasks(role) {
  if (role === 'emp') {
    const [receipts, sheets, stocks, mine] = await Promise.all([
      settle(getPendingReceipts('emp'), []), settle(getStockCountSheets(), []),
      settle(getStoreStocks('emp'), []), settle(getMyChangeRequests(), { content: [] }),
    ]);
    const open = sheets.filter((s) => s.status === 'OPEN');
    const low = stocks.filter((s) => s.low);
    const reqs = mine.content || [];
    return [
      { key: 'receipt', title: '수령 확인 대기', count: receipts.length, sub: receipts.length ? `발주 #${receipts[0].orderId}${receipts.length > 1 ? ` 외 ${receipts.length - 1}건` : ''}` : '도착한 배송이 없습니다', icon: ICON.truck, tone: 'navy', href: '/emp/receipts' },
      { key: 'count', title: '작성할 재고 실사', count: open.length, unit: '장', sub: open.length ? `${open[0].sheetCode} · ${open[0].lineCount}품목` : '오늘 실사표 없음', icon: ICON.receipt, tone: 'orange', href: '/emp/stock-counts' },
      { key: 'low', title: '매장 재고 부족', count: low.length, unit: '품목', sub: `전체 ${stocks.length}품목 중`, icon: ICON.box, tone: 'red', href: '/emp/stock-use', bar: { value: low.length, total: stocks.length, tone: 'red' } },
      { key: 'req', title: '진행 중인 내 요청', count: reqs.filter(isOpen).length, sub: `접수 ${reqs.filter((r) => r.status === 'REQUESTED').length} · 처리 중 ${reqs.filter((r) => r.status === 'IN_PROGRESS').length}`, icon: ICON.edit, tone: 'teal', href: '/emp/requests',
        parts: [{ label: '접수', value: reqs.filter((r) => r.status === 'REQUESTED').length, tone: 'navy' }, { label: '처리 중', value: reqs.filter((r) => r.status === 'IN_PROGRESS').length, tone: 'orange' }] },
    ];
  }
  if (role === 'owner') {
    const [submitted, inbox, receipts, stocks] = await Promise.all([
      settle(getOwnerStockCountSheets('SUBMITTED'), []), settle(getChangeRequestInbox(null), { content: [] }),
      settle(getPendingReceipts('owner'), []), settle(getStoreStocks('owner'), []),
    ]);
    const reqs = (inbox.content || []).filter(isOpen);
    const low = stocks.filter((s) => s.low);
    return [
      { key: 'approve', title: '실사 승인 대기', count: submitted.length, unit: '장', sub: submitted.length ? `${submitted[0].submittedByName} 제출 · 차이 ${submitted[0].mismatchCount}품목` : '승인할 실사표가 없습니다', icon: ICON.check, tone: 'orange', href: '/owner/stock-counts' },
      { key: 'req', title: '받은 수정 요청', count: reqs.length, sub: `접수 ${reqs.filter((r) => r.status === 'REQUESTED').length} · 처리 중 ${reqs.filter((r) => r.status === 'IN_PROGRESS').length}`, icon: ICON.edit, tone: 'navy', href: '/owner/requests',
        parts: [{ label: '접수', value: reqs.filter((r) => r.status === 'REQUESTED').length, tone: 'navy' }, { label: '처리 중', value: reqs.filter((r) => r.status === 'IN_PROGRESS').length, tone: 'orange' }] },
      { key: 'receipt', title: '수령 확인 대기', count: receipts.length, sub: receipts.length ? '도착하면 받은 수량을 확인해 주세요' : '도착한 배송이 없습니다', icon: ICON.truck, tone: 'teal', href: '/owner/receipts' },
      { key: 'low', title: '매장 재고 부족', count: low.length, unit: '품목', sub: `전체 ${stocks.length}품목 중`, icon: ICON.box, tone: 'red', href: '/owner/store-stocks', bar: { value: low.length, total: stocks.length, tone: 'red' } },
    ];
  }
  const [inbox, ordered, lowItems, ready] = await Promise.all([
    settle(getChangeRequestInbox(null), { content: [] }), settle(getListByStatuses(['ORDERED']), []),
    settle(getLowStockAlerts(), []), settle(getDeliveryList({ status: 'READY', size: 1 }), {}),
  ]);
  const reqs = (inbox.content || []).filter(isOpen);
  const out = lowItems.filter((i) => (i.currentStock ?? 0) === 0).length;
  return [
    { key: 'order', title: '발주 승인 대기', count: ordered.length, sub: '점주 결제가 끝난 발주', icon: ICON.order, tone: 'navy', href: '/admin/orders' },
    { key: 'req', title: '매장 수정 요청', count: reqs.length, sub: `배송 이상 ${reqs.filter((r) => r.category === 'DELIVERY_ISSUE').length} · 상품 정보 ${reqs.filter((r) => r.category === 'ITEM_INFO').length}`, icon: ICON.edit, tone: 'orange', href: '/admin/requests',
      parts: [{ label: '배송 이상', value: reqs.filter((r) => r.category === 'DELIVERY_ISSUE').length, tone: 'orange' }, { label: '상품 정보', value: reqs.filter((r) => r.category === 'ITEM_INFO').length, tone: 'navy' }] },
    { key: 'ship', title: '출고 대기 배송', count: ready.totalElements ?? 0, sub: '운송장 발급·배송 시작 전', icon: ICON.truck, tone: 'teal', href: '/admin/delivery' },
    { key: 'low', title: '본사 재고 부족', count: lowItems.length, unit: '품목', sub: `품절 ${out} · 부족 ${lowItems.length - out}`, icon: ICON.box, tone: 'red', href: '/admin/stock',
      parts: [{ label: '품절', value: out, tone: 'red' }, { label: '부족', value: lowItems.length - out, tone: 'orange' }] },
  ];
}

/** 대시보드 맨 위 "빠른 업무" — 급하게 처리할 화면으로 바로 가는 숫자 카드 4개 */
export default function QuickTasks({ role }) {
  const [tasks, setTasks] = useState(null);

  useEffect(() => {
    let alive = true;
    loadTasks(role).then((t) => { if (alive) setTasks(t); });
    return () => { alive = false; };
  }, [role]);

  const placeholders = [0, 1, 2, 3];
  return (
    <section>
      <h3 className="text-lg font-normal text-slate-900 pb-3 mb-4 border-b border-slate-900">빠른 업무</h3>
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 sm:gap-4">
        {tasks
          ? tasks.map(({ key, ...t }) => <QuickTaskCard key={key} {...t} />)
          : placeholders.map((i) => <QuickTaskCard key={i} title="불러오는 중" loading href="#" icon={null} />)}
      </div>
    </section>
  );
}
