import { useEffect, useMemo, useState } from 'react';
import { getOwnerOrderDetail, modifyAndApproveOwnerOrder, rejectOwnerDraft } from '../../api/orderOwnerApi';
import { Toast } from '../../utils/toast';
import Spinner from '../common/Spinner';
import PersonName from '../common/PersonName';

/**
 * 점주: 직원 기안(REQUESTED) 수량 조정·삭제 후 결제 승인 / 기안 반려
 */
export default function OrderModifyModal({ orderId, onClose, onDone }) {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [meta, setMeta] = useState(null);
  const [lines, setLines] = useState([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getOwnerOrderDetail(orderId)
      .then((detail) => {
        if (cancelled) return;
        setMeta(detail);
        setLines(
          (detail.items || []).map((it) => ({
            orderDetailId: it.orderDetailId,
            itemId: it.itemId,
            itemName: it.itemName,
            priceSnapshot: Number(it.priceSnapshot || 0),
            quantity: Number(it.quantity || 1),
          })),
        );
      })
      .catch(() => {
        Toast.error('발주 상세를 불러오지 못했습니다.');
        onClose();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [orderId, onClose]);

  const total = useMemo(
    () => lines.reduce((sum, l) => sum + l.priceSnapshot * l.quantity, 0),
    [lines],
  );

  const bump = (itemId, delta) => {
    setLines((prev) =>
      prev.map((l) => {
        if (l.itemId !== itemId) return l;
        const next = Math.max(1, l.quantity + delta);
        return { ...l, quantity: next };
      }),
    );
  };

  const removeLine = (itemId) => {
    setLines((prev) => prev.filter((l) => l.itemId !== itemId));
  };

  const handleApprove = async () => {
    if (lines.length === 0) {
      Toast.warning('결제할 품목이 없습니다. 기안 반려를 이용하세요.');
      return;
    }
    setSubmitting(true);
    try {
      await modifyAndApproveOwnerOrder(orderId, {
        items: lines.map((l) => ({ itemId: l.itemId, quantity: l.quantity })),
      });
      Toast.success('수정 및 결제 승인이 완료되었습니다. 본사 승인 대기로 전달됩니다.');
      onDone?.();
      onClose();
    } catch {
      /* interceptor toast */
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!window.confirm('이 직원 기안을 반려하시겠습니까? (미결제 상태라 예치금 차감은 없습니다)')) return;
    setSubmitting(true);
    try {
      await rejectOwnerDraft(orderId, { reason: '점주 기안 반려' });
      Toast.success('기안이 반려되었습니다.');
      onDone?.();
      onClose();
    } catch {
      /* interceptor */
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40">
        <Spinner size="lg" />
      </div>
    );
  }
  if (!meta) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl p-6 max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-800">기안 수정 및 결제 승인</h2>
            <p className="text-xs text-slate-500 mt-0.5">발주 #{meta.orderRequestId}</p>
          </div>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>

        <div className="grid grid-cols-2 gap-3 bg-slate-50 p-4 rounded-xl text-sm mb-4">
          <div>
            <span className="text-xs text-slate-500 block">신청자</span>
            <span className="font-semibold"><PersonName name={meta.employeeName} /></span>
          </div>
          <div>
            <span className="text-xs text-slate-500 block">신청일시</span>
            <span className="font-semibold">
              {meta.requestDate ? new Date(meta.requestDate).toLocaleString('ko-KR') : '-'}
            </span>
          </div>
        </div>

        <div className="overflow-y-auto flex-1 border border-slate-200 rounded-xl">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-xs text-slate-500 sticky top-0">
              <tr>
                <th className="px-3 py-2 text-left">품목</th>
                <th className="px-3 py-2 text-right">단가</th>
                <th className="px-3 py-2 text-center">수량</th>
                <th className="px-3 py-2 text-right">소계</th>
                <th className="px-3 py-2 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {lines.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-3 py-8 text-center text-slate-400">품목이 없습니다</td>
                </tr>
              ) : (
                lines.map((l) => (
                  <tr key={l.itemId}>
                    <td className="px-3 py-2.5 font-medium text-slate-800">{l.itemName}</td>
                    <td className="px-3 py-2.5 text-right tabular-nums">
                      {l.priceSnapshot.toLocaleString('ko-KR')}
                    </td>
                    <td className="px-3 py-2.5">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          type="button"
                          onClick={() => bump(l.itemId, -1)}
                          className="w-7 h-7 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 font-bold"
                        >
                          −
                        </button>
                        <span className="w-8 text-center font-bold tabular-nums">{l.quantity}</span>
                        <button
                          type="button"
                          onClick={() => bump(l.itemId, 1)}
                          className="w-7 h-7 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 font-bold"
                        >
                          +
                        </button>
                      </div>
                    </td>
                    <td className="px-3 py-2.5 text-right font-bold tabular-nums">
                      {(l.priceSnapshot * l.quantity).toLocaleString('ko-KR')}
                    </td>
                    <td className="px-3 py-2.5 text-center">
                      <button
                        type="button"
                        onClick={() => removeLine(l.itemId)}
                        className="text-xs font-bold text-rose-600 hover:bg-rose-50 px-2 py-1 rounded-lg"
                        title="품목 삭제"
                      >
                        [X]
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-4 flex items-center justify-between bg-emerald-50 border border-emerald-100 rounded-xl px-4 py-3">
          <span className="text-sm font-semibold text-emerald-800">총 결제 예정 예치금</span>
          <span className="text-xl font-bold text-emerald-700 tabular-nums">
            {total.toLocaleString('ko-KR')}원
          </span>
        </div>

        <div className="mt-4 flex gap-2">
          <button
            type="button"
            disabled={submitting}
            onClick={handleReject}
            className="flex-1 py-2.5 text-sm font-semibold rounded-xl border border-rose-200 text-rose-700 bg-rose-50 hover:bg-rose-100 disabled:opacity-60"
          >
            기안 반려
          </button>
          <button
            type="button"
            disabled={submitting || lines.length === 0}
            onClick={handleApprove}
            className="flex-[1.4] py-2.5 text-sm font-semibold rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {submitting ? '처리 중…' : '수정 및 결제 승인'}
          </button>
        </div>
      </div>
    </div>
  );
}
