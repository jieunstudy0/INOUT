import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getListByStatuses, bulkApprove } from '../api/orderAdmApi';
import { getDashboardSummary } from '../api/dashboardApi';
import { triggerAiAutoOrderAnalysis } from '../api/aiApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import client from '../api/apiClient';

const TABS = [
  { key: 'ALL',       label: '전체',      statuses: [],                       accent: 'slate'   },
  { key: 'PAID',      label: '승인 대기', statuses: ['PAID'],                 accent: 'blue'    },
  { key: 'PARTIAL',   label: '부분 처리', statuses: ['PARTIAL'],              accent: 'amber'   },
  { key: 'COMPLETED', label: '완료',      statuses: ['COMPLETED'],            accent: 'emerald' },
  { key: 'CLOSED',    label: '취소/반려', statuses: ['REJECTED', 'CANCELLED'], accent: 'rose'   },
];

const ORDER_STATUS_MAP = {
  REQUESTED: { label: '결제 대기', cls: 'bg-slate-100 text-slate-600'    },
  PAID:      { label: '승인 대기', cls: 'bg-blue-100 text-blue-700'      },
  PARTIAL:   { label: '부분 처리', cls: 'bg-amber-100 text-amber-700'    },
  COMPLETED: { label: '승인 완료', cls: 'bg-emerald-100 text-emerald-700'},
  REJECTED:  { label: '반려됨',   cls: 'bg-rose-100 text-rose-700'      },
  CANCELLED: { label: '취소됨',   cls: 'bg-slate-100 text-slate-500'    },
};

function OrderStatusBadge({ status }) {
  const cfg = ORDER_STATUS_MAP[status] || { label: status || '-', cls: 'bg-slate-100 text-slate-600' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cfg.cls}`}>
      {cfg.label}
    </span>
  );
}

function BulkResultPanel({ result, onClose }) {
  if (!result) return null;
  const { successCount, autoRejectCount, failureCount, failures } = result;
  const total      = (successCount ?? 0) + (autoRejectCount ?? 0) + (failureCount ?? 0);
  const allSuccess = autoRejectCount === 0 && failureCount === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className={`px-6 py-5 ${allSuccess ? 'bg-emerald-500' : 'bg-amber-500'}`}>
          <div className="flex items-center gap-3">
            <svg className="w-8 h-8 text-white shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              {allSuccess
                ? <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                : <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              }
            </svg>
            <div>
              <h3 className="text-lg font-bold text-white">일괄 승인 처리 완료</h3>
              <p className="text-sm text-white/80">총 {total}건 처리됨</p>
            </div>
          </div>
        </div>
        <div className="px-6 py-5 space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-emerald-50 border border-emerald-100 rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-emerald-600">{successCount}</p>
              <p className="text-xs text-emerald-600 font-medium mt-0.5">승인 완료</p>
            </div>
            <div className="bg-amber-50 border border-amber-100 rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-amber-600">{autoRejectCount}</p>
              <p className="text-xs text-amber-600 font-medium mt-0.5">재고부족 반려</p>
            </div>
            <div className="bg-rose-50 border border-rose-100 rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-rose-600">{failureCount}</p>
              <p className="text-xs text-rose-600 font-medium mt-0.5">처리 실패</p>
            </div>
          </div>
          {autoRejectCount > 0 && (
            <div className="flex items-start gap-2.5 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
              <svg className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
              </svg>
              <div>
                <p className="text-xs font-semibold text-amber-800">재고 부족 자동 반려 안내</p>
                <p className="text-xs text-amber-700 mt-0.5">
                  재고가 부족한 {autoRejectCount}건은 자동으로 반려 처리되었습니다.
                  각 발주의 <strong>예치금은 전액 자동 환불</strong>되었습니다.
                </p>
              </div>
            </div>
          )}
          {failures?.length > 0 && (
            <div className="border border-rose-100 rounded-xl overflow-hidden">
              <div className="bg-rose-50 px-4 py-2 text-xs font-semibold text-rose-700">처리 실패 내역</div>
              <ul className="divide-y divide-rose-50">
                {failures.map((f) => (
                  <li key={f.orderId} className="px-4 py-2.5 flex items-center justify-between">
                    <span className="text-xs font-medium text-slate-700">주문 #{f.orderId}</span>
                    <span className="text-xs text-rose-600">{f.reason}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          <button onClick={onClose}
            className="w-full py-2.5 text-sm font-semibold rounded-xl bg-slate-800 text-white hover:bg-slate-700 active:scale-[0.98] transition-all">
            확인
          </button>
        </div>
      </div>
    </div>
  );
}

function OrderTable({ orders, checkedIds, onCheckAll, onCheck }) {
  const navigate = useNavigate();
  const allChecked  = orders.length > 0 && checkedIds.size === orders.length;
  const someChecked = checkedIds.size > 0 && !allChecked;

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR');
  };
  const formatCurrency = (val) =>
    val != null ? `${Number(val).toLocaleString('ko-KR')}원` : '-';

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-5 py-3 w-10">
                <input type="checkbox" checked={allChecked}
                  ref={(el) => { if (el) el.indeterminate = someChecked; }}
                  onChange={(e) => onCheckAll(e.target.checked)}
                  className="w-4 h-4 rounded text-indigo-600 border-slate-300 focus:ring-indigo-500 cursor-pointer" />
              </th>
              {['주문번호', '요청자', '대표 품목', '총 금액', '주문일시', '상태', ''].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {orders.map((order) => {
              const checked = checkedIds.has(order.orderRequestId);
              return (
                <tr key={order.orderRequestId}
                  className={`hover:bg-slate-50 transition-colors ${checked ? 'bg-indigo-50/30' : ''}`}>
                  <td className="px-5 py-3.5">
                    <input type="checkbox" checked={checked}
                      onChange={(e) => onCheck(order.orderRequestId, e.target.checked)}
                      className="w-4 h-4 rounded text-indigo-600 border-slate-300 focus:ring-indigo-500 cursor-pointer" />
                  </td>
                  <td className="px-4 py-3.5 text-sm font-semibold text-slate-700">#{order.orderRequestId}</td>
                  <td className="px-4 py-3.5">
                    <p className="text-sm font-medium text-slate-800">{order.employeeName || '-'}</p>
                    <p className="text-xs text-slate-400 mt-0.5">{order.storeName || '-'}</p>
                  </td>
                  <td className="px-4 py-3.5">
                    <p className="text-sm text-slate-700">{order.representativeItemName || '-'}</p>
                    {order.itemCount > 1 && (
                      <p className="text-xs text-slate-400 mt-0.5">외 {order.itemCount - 1}건</p>
                    )}
                  </td>
                  <td className="px-4 py-3.5 text-sm font-semibold text-slate-800 text-right whitespace-nowrap">
                    {formatCurrency(order.totalPrice)}
                  </td>
                  <td className="px-4 py-3.5 text-xs text-slate-500 whitespace-nowrap">{formatDate(order.requestDate)}</td>
                  <td className="px-4 py-3.5"><OrderStatusBadge status={order.status} /></td>
                  <td className="px-4 py-3.5">
                    <button
                      onClick={() => navigate(`/admin/orders/${order.orderRequestId}`)}
                      className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-50 border border-slate-200 text-slate-600 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200 transition-all whitespace-nowrap">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      상세보기
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function OrderAdmPage() {
  const [orders, setOrders]           = useState([]);
  const [loading, setLoading]         = useState(true);
  const [activeTab, setActiveTab]     = useState('ALL');
  const [checkedIds, setCheckedIds]   = useState(new Set());
  const [bulkProcessing, setBulkProc] = useState(false);
  const [bulkResult, setBulkResult]   = useState(null);
  const [summaryData, setSummaryData] = useState(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [aiOrderRunning, setAiOrderRunning] = useState(false);

  const loadOrders = useCallback((tab) => {
    const tabCfg = TABS.find((t) => t.key === tab) || TABS[0];
    setLoading(true);
    setCheckedIds(new Set());
    getListByStatuses(tabCfg.statuses)
      .then((data) => setOrders(Array.isArray(data) ? data : []))
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  }, []);

  const loadSummary = useCallback(() => {
    getDashboardSummary()
      .then((data) => setSummaryData(data))
      .catch(() => {});
  }, []);

  useEffect(() => { loadOrders(activeTab); }, [activeTab, loadOrders]);
  useEffect(() => { loadSummary(); }, [loadSummary]);

  const handleTabChange = (tab) => { setActiveTab(tab); setCheckedIds(new Set()); };
  const handleCheckAll  = (checked) => setCheckedIds(checked ? new Set(orders.map((o) => o.orderRequestId)) : new Set());
  const handleCheck = (id, checked) => setCheckedIds((prev) => {
    const next = new Set(prev);
    checked ? next.add(id) : next.delete(id);
    return next;
  });

  const handleBulkApprove = async () => {
    if (checkedIds.size === 0) return;
    const paidOrders = orders.filter((o) => checkedIds.has(o.orderRequestId) && o.status === 'PAID');
    if (paidOrders.length === 0) {
      Toast.warning('결제 완료(승인 대기) 상태의 발주만 일괄 승인할 수 있습니다.');
      return;
    }
    const nonPaid = checkedIds.size - paidOrders.length;
    if (nonPaid > 0) Toast.info(`${nonPaid}건의 비적격 상태 발주는 제외하고 ${paidOrders.length}건만 처리합니다.`);
    setBulkProc(true);
    try {
      const result = await bulkApprove(paidOrders.map((o) => o.orderRequestId));
      setBulkResult(result);
      loadOrders(activeTab);
      loadSummary();
    } catch { /* toasted */ }
    finally { setBulkProc(false); }
  };

  const handleRefresh = () => {
    loadOrders(activeTab);
    loadSummary();
    Toast.info('발주 현황을 새로고침했습니다.');
  };

  const handleAiAutoOrderDraft = async () => {
    setAiOrderRunning(true);
    try {
      const data = await triggerAiAutoOrderAnalysis();
      Toast.success(data?.message || 'AI 발주 초안 생성이 완료되었습니다.');
      loadOrders(activeTab);
      loadSummary();
    } catch {
      /* interceptor */
    } finally {
      setAiOrderRunning(false);
    }
  };

  const handleExcelDownload = async () => {
    setIsDownloading(true);
    try {   
      const response = await client.get('/admin/orders/excel', {
        responseType: 'blob',
      });

      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      const date = new Date();
      const dateString = date.getFullYear() +
        String(date.getMonth() + 1).padStart(2, '0') +
        String(date.getDate()).padStart(2, '0');

      link.download = `발주내역리스트_${dateString}.xlsx`;
      document.body.appendChild(link);
      link.click();
      
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      Toast.success('엑셀 파일이 성공적으로 다운로드되었습니다.');

    } catch (error) {
      console.error('Excel download error:', error);
      Toast.error('엑셀 다운로드에 실패했습니다. 권한을 확인해주세요.');
    } finally {
      setIsDownloading(false);
    }
  };

  const checkedPaidCount = orders.filter((o) => checkedIds.has(o.orderRequestId) && o.status === 'PAID').length;

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">발주 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            결제 완료 발주 승인·반려 처리
          </p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <button
            type="button"
            onClick={handleAiAutoOrderDraft}
            disabled={aiOrderRunning}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-60 shadow-sm transition-all"
          >
            {aiOrderRunning ? (
              <><Spinner size="sm" className="text-white" /> 초안 생성 중…</>
            ) : (
              <>AI 발주 초안 생성</>
            )}
          </button>
          <button 
            onClick={handleExcelDownload} 
            disabled={isDownloading}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-all shadow-sm disabled:opacity-60 disabled:cursor-not-allowed">
            {isDownloading ? (
              <Spinner size="sm" className="text-emerald-600" />
            ) : (
              <svg className="w-4 h-4 text-emerald-600" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
              </svg>
            )}
            {isDownloading ? '다운로드 중...' : '엑셀 다운로드'}
          </button>
          <button onClick={handleRefresh}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-all shadow-sm">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
            새로고침
          </button>
        </div>
      </div>

      {/* ── 상단 발주 요약 패널 ── */}
      {summaryData && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-slate-500 mb-1">총 누적 발주</p>
            <p className="text-2xl font-bold text-slate-800">{summaryData.totalOrderCount?.toLocaleString() || 0}건</p>
          </div>
          <div className="bg-indigo-50 p-5 rounded-2xl border border-indigo-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-indigo-600 mb-1">금일 신규 발주</p>
            <p className="text-2xl font-bold text-indigo-700">{summaryData.todayNewOrderCount?.toLocaleString() || 0}건</p>
          </div>
          <div className="bg-emerald-50 p-5 rounded-2xl border border-emerald-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-emerald-600 mb-1">승인 완료</p>
            <p className="text-2xl font-bold text-emerald-700">{summaryData.completedOrderCount?.toLocaleString() || 0}건</p>
          </div>
          <div className="bg-rose-50 p-5 rounded-2xl border border-rose-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-rose-600 mb-1">반려 처리</p>
            <p className="text-2xl font-bold text-rose-700">{summaryData.rejectedOrderCount?.toLocaleString() || 0}건</p>
          </div>
        </div>
      )}

      {/* 일괄 승인 액션 바 */}
      {checkedIds.size > 0 && (
        <div className="flex items-center justify-between bg-indigo-50 border border-indigo-200 rounded-2xl px-5 py-3.5">
          <div className="flex items-center gap-2 text-sm text-indigo-700">
            <svg className="w-4 h-4 text-indigo-500" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>
              <strong>{checkedIds.size}건</strong> 선택됨
              {checkedPaidCount < checkedIds.size && (
                <span className="ml-1 text-xs text-indigo-500">(승인 가능: {checkedPaidCount}건)</span>
              )}
            </span>
          </div>
          <div className="flex gap-2">
            <button onClick={() => setCheckedIds(new Set())}
              className="px-3 py-1.5 text-xs font-medium rounded-lg text-slate-600 hover:bg-slate-100 transition-colors">
              선택 해제
            </button>
            <button onClick={handleBulkApprove} disabled={bulkProcessing || checkedPaidCount === 0}
              className="flex items-center gap-2 px-5 py-2 text-sm font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed active:scale-[0.98] transition-all shadow-sm">
              {bulkProcessing
                ? <><Spinner size="sm" className="text-white" /> 처리 중...</>
                : <>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    선택 발주 일괄 승인 ({checkedPaidCount}건)
                  </>
              }
            </button>
          </div>
        </div>
      )}

      {/* 탭 필터 */}
      <div className="flex flex-wrap gap-1.5 bg-white border border-slate-200 rounded-2xl p-1.5 shadow-sm w-fit">
        {TABS.map((tab) => (
          <button key={tab.key} onClick={() => handleTabChange(tab.key)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              tab.key === activeTab
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
            }`}>
            {tab.label}
          </button>
        ))}
      </div>

      {loading && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20 gap-3 text-slate-400">
          <Spinner size="lg" /><span className="text-sm">발주 목록을 불러오는 중...</span>
        </div>
      )}
      {!loading && orders.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message="해당 조건의 발주 내역이 없습니다." />
        </div>
      )}
      {!loading && orders.length > 0 && (
        <OrderTable orders={orders} checkedIds={checkedIds}
          onCheckAll={handleCheckAll} onCheck={handleCheck} />
      )}

      {bulkResult && (
        <BulkResultPanel result={bulkResult} onClose={() => setBulkResult(null)} />
      )}
    </div>
  );
}