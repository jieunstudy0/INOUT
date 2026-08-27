import { useState, useEffect, useCallback, Fragment } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getDetail, processItems, approveAiSuggestedItems } from '../api/orderAdmApi.js';
import { getDeliveryByOrder, generateWaybill, startShipping } from '../api/deliveryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import DeliveryTrackingModal from '../components/delivery/DeliveryTrackingModal';
import PersonName from '../components/common/PersonName';

const ORDER_STATUS_MAP = {
  REQUESTED: { label: '직원 기안', cls: 'bg-slate-100 text-slate-600'    },
  ORDERED:   { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
  APPROVED:  { label: '최종 승인', cls: 'bg-emerald-100 text-emerald-700'},
  PAID:      { label: '본사 승인 대기', cls: 'bg-blue-100 text-blue-700' },
  PARTIAL:   { label: '부분 처리', cls: 'bg-amber-100 text-amber-700'    },
  COMPLETED: { label: '최종 승인', cls: 'bg-emerald-100 text-emerald-700'},
  REJECTED:  { label: '반려됨',   cls: 'bg-rose-100 text-rose-700'      },
  CANCELLED: { label: '취소됨',   cls: 'bg-slate-100 text-slate-500'    },
};

const DETAIL_STATUS_MAP = {
  WAITING:  { label: '대기 중', cls: 'bg-slate-100 text-slate-600'    },
  APPROVED: { label: '승인',   cls: 'bg-emerald-100 text-emerald-700' },
  DELAYED:  { label: '지연',   cls: 'bg-amber-100 text-amber-700'     },
  REJECTED: { label: '반려',   cls: 'bg-rose-100 text-rose-700'        },
};

function OrderStatusBadge({ status }) {
  const cfg = ORDER_STATUS_MAP[status] || { label: status || '-', cls: 'bg-slate-100 text-slate-600' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cfg.cls}`}>
      {cfg.label}
    </span>
  );
}

function DetailStatusBadge({ status }) {
  const cfg = DETAIL_STATUS_MAP[status] || { label: status || '-', cls: 'bg-slate-100 text-slate-600' };
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${cfg.cls}`}>
      {cfg.label}
    </span>
  );
}

export default function OrderAdmDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProc] = useState(null);
  const [delivery, setDelivery] = useState(null);
  const [issuing, setIssuing] = useState(false);
  const [starting, setStarting] = useState(false);
  const [trackOpen, setTrackOpen] = useState(false);
  // 백엔드 직렬화 형태(isAiSuggested/aiSuggested) 차이를 모두 허용
  const isAiSuggestedItem = (item) => (item?.isAiSuggested ?? item?.aiSuggested ?? false);

  const loadDelivery = useCallback(() => {
    if (!orderId) return;
    getDeliveryByOrder(orderId)
      .then(setDelivery)
      .catch(() => setDelivery(null));
  }, [orderId]);

  const loadDetail = useCallback(() => {
    setLoading(true);
    getDetail(orderId)
      .then((data) => setDetail(data))
      .catch(() => {
        Toast.error('발주 정보를 불러오지 못했습니다.');
        navigate('/admin/orders');
      })
      .finally(() => setLoading(false));
  }, [orderId, navigate]);

  useEffect(() => { loadDetail(); }, [loadDetail]);
  useEffect(() => { loadDelivery(); }, [loadDelivery]);

  const handleGenerateWaybill = async () => {
    if (!delivery?.deliveryId) return;
    setIssuing(true);
    try {
      const res = await generateWaybill(delivery.deliveryId);
      setDelivery(res);
      Toast.success('CJ대한통운 운송장이 발급되었습니다.');
    } catch { /* */ }
    finally { setIssuing(false); }
  };

  const handleStartShipping = async () => {
    if (!delivery?.trackingNumber) {
      Toast.warning('먼저 운송장을 발급하거나 입력해 주세요.');
      return;
    }
    setStarting(true);
    try {
      const res = await startShipping(orderId, delivery.trackingNumber);
      setDelivery(res);
      Toast.success('배송이 시작되었습니다.');
    } catch { /* */ }
    finally { setStarting(false); }
  };

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR');
  };
  const formatCurrency = (val) =>
    val != null ? `${Number(val).toLocaleString('ko-KR')}원` : '-';

  const handleProcessItem = async (orderDetailId, status) => {
    setProc(orderDetailId);
    try {
      await processItems(orderId, [{ orderDetailId, status }]);
      const label = status === 'APPROVED' ? '승인' : status === 'REJECTED' ? '반려' : '대기';
      Toast.success(`품목이 ${label} 처리되었습니다.`);
      loadDetail();
      loadDelivery();
    } catch { /* api client already toasted */ }
    finally { setProc(null); }
  };

  const handleApproveAll = async () => {
    if (!detail?.items) return;
    const waitingItems = detail.items.filter((i) => !isAiSuggestedItem(i) && (i.status === 'WAITING' || i.status === 'DELAYED'));
    if (waitingItems.length === 0) { Toast.info('처리 가능한 대기 중 품목이 없습니다.'); return; }
    setProc('ALL');
    try {
      await processItems(orderId, waitingItems.map((i) => ({ orderDetailId: i.orderDetailId, status: 'APPROVED' })));
      Toast.success(`${waitingItems.length}개 품목이 모두 승인 처리되었습니다.`);
      loadDetail();
      loadDelivery();
    } catch { /* toasted */ }
    finally { setProc(null); }
  };

  const handleAiDecision = async (orderDetailId, approve) => {
    setProc(orderDetailId);
    try {
      await approveAiSuggestedItems(orderId, [{ orderDetailId, approve }]);
      Toast.success(approve ? 'AI 제안 품목이 승인되었습니다.' : 'AI 제안 품목이 반려되었습니다.');
      loadDetail();
      loadDelivery();
    } catch { /* api client already toasted */ }
    finally { setProc(null); }
  };

  const handleAiApproveAll = async () => {
    if (!detail?.items) return;
    const aiWaitingItems = detail.items.filter((i) => isAiSuggestedItem(i) && (i.status === 'WAITING' || i.status === 'DELAYED'));
    if (aiWaitingItems.length === 0) { Toast.info('처리 가능한 AI 제안 품목이 없습니다.'); return; }
    setProc('AI_ALL');
    try {
      await approveAiSuggestedItems(
        orderId,
        aiWaitingItems.map((i) => ({ orderDetailId: i.orderDetailId, approve: true })),
      );
      Toast.success(`AI 제안 품목 ${aiWaitingItems.length}건이 모두 승인되었습니다.`);
      loadDetail();
      loadDelivery();
    } catch { /* toasted */ }
    finally { setProc(null); }
  };

  if (loading) return <div className="flex justify-center py-32"><Spinner size="lg" /></div>;
  if (!detail) return <EmptyState message="발주 정보를 찾을 수 없습니다." />;

  const waitingCount = detail.items?.filter((i) => !isAiSuggestedItem(i) && (i.status === 'WAITING' || i.status === 'DELAYED')).length ?? 0;
  const aiWaitingCount = detail.items?.filter((i) => isAiSuggestedItem(i) && (i.status === 'WAITING' || i.status === 'DELAYED')).length ?? 0;
  const hasAiSuggestedOrder = detail.items?.some((i) => isAiSuggestedItem(i)) ?? false;

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/admin/orders')} className="text-slate-400 hover:text-indigo-600 p-2 bg-white rounded-full shadow-sm border border-slate-200">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div>
          <h2 className="text-xl font-bold text-slate-800">발주 상세 처리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            #{detail.orderRequestId} · {detail.storeName || '본점 (소속 없음)'} ·{' '}
            <PersonName name={detail.employeeName} />
          </p>
        </div>
        <div className="ml-auto flex items-center gap-2">
          {hasAiSuggestedOrder && (
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-indigo-100 text-indigo-700 border border-indigo-200">
              ✦ AI 추천 발주
            </span>
          )}
          {detail.inboundStatusLabel && (
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-sky-100 text-sky-700 border border-sky-200">
              {detail.inboundStatusLabel}
            </span>
          )}
          {aiWaitingCount > 0 && (
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-teal-100 text-teal-700 border border-teal-200">
              AI 제안 대기 {aiWaitingCount}건
            </span>
          )}
          <OrderStatusBadge status={detail.status} />
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-px bg-slate-100">
          {[
            { label: '매장명',   value: detail.storeName || '본점 (소속 없음)' },
            { label: '신청자',   value: <PersonName name={detail.employeeName} /> },
            { label: '신청일시', value: formatDate(detail.requestDate) },
            { label: '총 금액',  value: formatCurrency(detail.totalPrice) },
          ].map((row) => (
            <div key={row.label} className="bg-white px-6 py-4">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">{row.label}</span>
              <p className="text-base font-semibold text-slate-800 mt-1">{row.value}</p>
            </div>
          ))}
        </div>
        {detail.aiSuggestedOrder && (
          <div className="px-6 py-3 bg-indigo-50/70 border-t border-indigo-100">
            <p className="text-xs text-indigo-800">
              <span className="font-semibold text-indigo-600 mr-2">공급처</span>
              {detail.vendorName || '(주)본사지정협력사'}
            </p>
            <p className="text-xs text-indigo-800 mt-1">
              <span className="font-semibold text-indigo-600 mr-2">예상 입고일</span>
              {detail.expectedInboundAt ? formatDate(detail.expectedInboundAt) : '-'}
            </p>
          </div>
        )}
        {detail.rejectReason && (
          <div className="px-6 py-3 bg-rose-50 border-t border-rose-100 flex items-start gap-2">
            <svg className="w-4 h-4 text-rose-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
            <p className="text-xs text-rose-700"><span className="font-semibold">반려 사유: </span>{detail.rejectReason}</p>
          </div>
        )}
      </div>

      {delivery && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div>
              <h3 className="text-sm font-bold text-slate-800">배송 / 운송장</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                상태: {delivery.status}
                {delivery.carrier ? ` · ${delivery.carrier}` : ''}
              </p>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {delivery.status === 'READY' && (
                <>
                  <button
                    type="button"
                    onClick={handleGenerateWaybill}
                    disabled={issuing || starting}
                    className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold rounded-xl bg-amber-50 text-amber-800 border border-amber-200 hover:bg-amber-100 disabled:opacity-50"
                  >
                    {issuing ? <Spinner size="sm" /> : '📦'} 택배사 연동 자동발급
                  </button>
                  {delivery.trackingNumber && (
                    <button
                      type="button"
                      onClick={handleStartShipping}
                      disabled={starting || issuing}
                      className="px-3 py-2 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
                    >
                      {starting ? '처리 중…' : '발송 시작'}
                    </button>
                  )}
                </>
              )}
              {delivery.trackingNumber && (
                <button
                  type="button"
                  onClick={() => setTrackOpen(true)}
                  className="inline-flex items-center gap-1 px-3 py-2 text-xs font-semibold rounded-xl bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-100"
                >
                  🚚 배송 조회
                </button>
              )}
            </div>
          </div>
          {delivery.trackingNumber ? (
            <p className="font-mono text-sm text-slate-800 bg-slate-50 border border-slate-100 rounded-xl px-3 py-2 w-fit">
              {delivery.trackingNumber}
            </p>
          ) : (
            <p className="text-xs text-slate-400">운송장 번호가 아직 없습니다. 자동발급 또는 배송 관리 화면에서 등록하세요.</p>
          )}
        </div>
      )}

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between flex-wrap gap-2">
          <div>
            <h3 className="text-lg font-bold text-slate-800">신청 품목 내역</h3>
            {aiWaitingCount > 0 && (
              <p className="text-xs text-teal-600 mt-0.5">AI 제안 품목은 사유를 확인한 뒤 승인·반려해 주세요.</p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {aiWaitingCount > 0 && (
              <button disabled={!!processing} onClick={handleAiApproveAll}
                className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-60 transition-all shadow-sm">
                {processing === 'AI_ALL' ? <Spinner size="sm" className="text-white" /> : null}
                AI 제안 전체 승인 ({aiWaitingCount}건)
              </button>
            )}
            {waitingCount > 0 && (
              <button disabled={!!processing} onClick={handleApproveAll}
                className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-60 transition-all shadow-sm">
                {processing === 'ALL' ? <Spinner size="sm" className="text-white" /> : (
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                )}
                대기 품목 전체 승인 ({waitingCount}건)
              </button>
            )}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-100">
            <thead className="bg-slate-50">
              <tr>
                {['상품명', '수량', '단가', '소계', '상태', '처리'].map((h) => (
                  <th key={h} className="px-6 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 bg-white">
              {detail.items?.map((item) => {
                const isThis = processing === item.orderDetailId;
                const isAiSuggested = isAiSuggestedItem(item);
                const aiEditable = isAiSuggested && (item.status === 'WAITING' || item.status === 'DELAYED');

                if (isAiSuggested) {
                  return (
                    <Fragment key={item.orderDetailId}>
                      <tr className="hover:bg-teal-50/40 transition-colors bg-teal-50/20">
                        <td className="px-6 py-4 text-sm font-medium text-slate-800 align-top">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-teal-100 text-teal-700 border border-teal-200">
                              AI 제안
                            </span>
                            <span>{item.itemName}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-slate-600 align-top">{(item.quantity ?? 0).toLocaleString()}</td>
                        <td className="px-6 py-4 text-sm text-slate-600 align-top">{formatCurrency(item.priceSnapshot)}</td>
                        <td className="px-6 py-4 text-sm font-semibold text-slate-700 align-top">{formatCurrency(item.subTotal)}</td>
                        <td className="px-6 py-4 align-top"><DetailStatusBadge status={item.status} /></td>
                        <td className="px-6 py-4 align-top">
                          {aiEditable ? (
                            <div className="flex items-center gap-1.5 flex-wrap">
                              <button disabled={!!processing}
                                onClick={() => handleAiDecision(item.orderDetailId, true)}
                                className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-50 transition-colors">
                                {isThis ? '...' : '승인'}
                              </button>
                              <button disabled={!!processing}
                                onClick={() => handleAiDecision(item.orderDetailId, false)}
                                className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 disabled:opacity-50 transition-colors">
                                {isThis ? '...' : '반려'}
                              </button>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-400">처리 완료</span>
                          )}
                        </td>
                      </tr>
                      {!!item.aiReason?.trim() && (
                        <tr className="bg-indigo-50/50">
                          <td colSpan={6} className="py-2 px-4 text-xs text-indigo-900 border-b border-indigo-100">
                            <span className="font-semibold text-indigo-600 mr-2">✦ AI 제안 근거:</span>
                            {item.aiReason}
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                }

                return (
                  <tr key={item.orderDetailId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 text-sm font-medium text-slate-800">{item.itemName}</td>
                    <td className="px-6 py-4 text-sm text-slate-600">{(item.quantity ?? 0).toLocaleString()}</td>
                    <td className="px-6 py-4 text-sm text-slate-600">{formatCurrency(item.priceSnapshot)}</td>
                    <td className="px-6 py-4 text-sm font-semibold text-slate-700">{formatCurrency(item.subTotal)}</td>
                    <td className="px-6 py-4"><DetailStatusBadge status={item.status} /></td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <button disabled={!!processing || item.status === 'APPROVED'}
                          onClick={() => handleProcessItem(item.orderDetailId, 'APPROVED')}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 disabled:opacity-50 transition-colors">
                          {isThis && processing !== 'ALL' ? '...' : '승인'}
                        </button>
                        <button disabled={!!processing || item.status === 'REJECTED'}
                          onClick={() => handleProcessItem(item.orderDetailId, 'REJECTED')}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 disabled:opacity-50 transition-colors">
                          {isThis && processing !== 'ALL' ? '...' : '반려'}
                        </button>
                        <button disabled={!!processing || item.status === 'WAITING'}
                          onClick={() => handleProcessItem(item.orderDetailId, 'WAITING')}
                          className="px-2.5 py-1 text-[11px] font-semibold rounded-lg bg-slate-100 text-slate-600 border border-slate-200 hover:bg-slate-200 disabled:opacity-50 transition-colors">
                          {isThis && processing !== 'ALL' ? '...' : '대기'}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <DeliveryTrackingModal
        open={trackOpen}
        onClose={() => setTrackOpen(false)}
        carrier={delivery?.carrier}
        trackingNumber={delivery?.trackingNumber}
      />
    </div>
  );
}
