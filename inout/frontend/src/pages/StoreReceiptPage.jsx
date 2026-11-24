import { useState, useEffect, useCallback } from 'react';
import { getPendingReceipts, receiveDelivery } from '../api/storeStockApi';
import { Toast } from '../utils/toast';
import { isStaleDataError } from '../utils/resultCodes';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { formatDateTime } from '../utils/stockCount';
import ChangeRequestForm from '../components/request/ChangeRequestForm';
import { confirmDialog } from '../utils/dialog';

/**
 * 배송 수령 확인 (직원·점주 공용, role = 'emp' | 'owner').
 * 물건이 도착하면 품목마다 실제로 받은 수량을 입력한다. 받은 만큼 매장 재고가 늘어난다.
 */
export default function StoreReceiptPage({ role = 'emp' }) {
  const [receipts, setReceipts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openId, setOpenId] = useState(null);
  const [qty, setQty] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [issue, setIssue] = useState(null); // 이상 신고 미리 채울 값

  const load = useCallback(() => {
    setLoading(true);
    getPendingReceipts(role)
      .then((data) => setReceipts(data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [role]);

  useEffect(() => { load(); }, [load]);

  const open = (r) => {
    setOpenId(r.orderId);
    const init = {};
    r.lines.forEach((l) => { init[l.orderDetailId] = String(l.orderedQty); });
    setQty(init);
  };

  const submit = async (r) => {
    const lines = r.lines.map((l) => ({ orderDetailId: l.orderDetailId, receivedQty: Number(qty[l.orderDetailId] || 0) }));
    const invalid = r.lines.find((l) => {
      const v = Number(qty[l.orderDetailId]);
      return qty[l.orderDetailId] === '' || Number.isNaN(v) || v < 0 || v > l.orderedQty;
    });
    if (invalid) return Toast.warning(`[${invalid.itemName}] 수령 수량은 0 ~ ${invalid.orderedQty}개로 입력해 주세요.`);

    const short = r.lines.filter((l) => Number(qty[l.orderDetailId]) < l.orderedQty);
    const msg = short.length
      ? `덜 받은 품목이 ${short.length}개 있습니다.\n${short.map((l) => `· ${l.itemName}: ${l.orderedQty} → ${qty[l.orderDetailId]}`).join('\n')}\n\n받은 수량만 매장 재고에 더합니다. 확인할까요?`
      : '발주 수량대로 모두 받았습니다. 수령 확인할까요?';
    if (!await confirmDialog(msg)) return;

    setSubmitting(true);
    try {
      const result = await receiveDelivery(role, r.orderId, lines);
      Toast.success(result.shortItemCount
        ? `수령 확인했습니다. 부족한 ${result.shortItemCount}품목은 기록해 두었습니다.`
        : '수령 확인했습니다. 매장 재고에 반영되었습니다.');
      setOpenId(null);
      load();
      if (short.length && await confirmDialog('덜 받은 품목을 본사에 배송 이상으로 신고할까요?')) {
        openIssue(r, short.map((l) => `· ${l.itemName}: 발주 ${l.orderedQty} / 받음 ${qty[l.orderDetailId]}`).join('\n'));
      }
    } catch (err) {
      if (isStaleDataError(err) || err?.resultCode === 'ALREADY_RECEIVED') load();
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * 배송 이상 신고: 발주를 대상으로 본사에 보낸다. 사진을 함께 올릴 수 있다.
   * 덜 받은 품목이 있으면 '수량 부족'을 골라 두고 목록을 채운다. 그 밖에는 직원이 종류(파손·오배송·기타 등)를 고른다.
   */
  const openIssue = (r, detail = '') => setIssue({
    category: 'DELIVERY_ISSUE',
    issueType: detail ? 'SHORTAGE' : null,
    detail,
    refType: 'ORDER',
    refId: r.orderId,
    refLabel: `발주 #${r.orderId}`,
  });

  return (
    <div className="space-y-4 max-w-3xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">배송 수령 확인</h2>
        <p className="text-sm text-slate-500 mt-0.5">물건이 도착하면 실제로 받은 수량을 확인해 주세요. 받은 만큼 매장 재고가 늘어납니다.</p>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : receipts.length === 0 ? (
        <EmptyState message="수령 확인할 배송이 없습니다." />
      ) : (
        <ul className="space-y-3">
          {receipts.map((r) => (
            <li key={r.orderId} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <button onClick={() => (openId === r.orderId ? setOpenId(null) : open(r))}
                className="w-full text-left px-4 py-3 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="text-sm font-bold text-slate-800">발주 #{r.orderId} · {r.lines.length}품목</div>
                  <div className="text-xs text-slate-500 mt-0.5 truncate">
                    {r.deliveryStatus === 'SHIPPING' ? '배송 중' : '배송 완료'}
                    {r.carrier ? ` · ${r.carrier}` : ''}{r.trackingNumber ? ` ${r.trackingNumber}` : ''}
                    {` · 출발 ${formatDateTime(r.shippedAt)}`}
                  </div>
                </div>
                <span className="shrink-0 px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-50 text-indigo-700">
                  {openId === r.orderId ? '닫기' : '수령 확인'}
                </span>
              </button>

              {openId === r.orderId && (
                <div className="border-t border-slate-100 px-4 py-3 space-y-2">
                  {r.lines.map((l) => {
                    const v = qty[l.orderDetailId] ?? '';
                    const shortBy = v === '' ? 0 : l.orderedQty - Number(v);
                    return (
                      <div key={l.orderDetailId} className="flex items-center justify-between gap-3">
                        <div className="min-w-0">
                          <div className="text-sm font-medium text-slate-800 truncate">{l.itemName}</div>
                          <div className="text-xs text-slate-400">발주 {l.orderedQty}{l.unitDescription ? ` ${l.unitDescription}` : '개'}</div>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          {shortBy > 0 && <span className="text-xs font-semibold text-rose-600">-{shortBy}</span>}
                          <input type="number" inputMode="numeric" min="0" max={l.orderedQty} value={v}
                            onChange={(e) => setQty((prev) => ({ ...prev, [l.orderDetailId]: e.target.value }))}
                            className={`w-20 px-2 py-2 text-sm text-right border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/40 ${shortBy > 0 ? 'border-rose-300' : 'border-slate-300'}`} />
                        </div>
                      </div>
                    );
                  })}
                  <button onClick={() => submit(r)} disabled={submitting}
                    className="w-full mt-2 py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
                    {submitting ? '처리 중...' : '받은 수량으로 수령 확인'}
                  </button>
                  <button onClick={() => openIssue(r)} className="w-full py-2.5 text-xs font-semibold rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50">
                    파손·오배송 등 이상 신고 (사진 첨부)
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {issue && <ChangeRequestForm role={role} preset={issue} onClose={() => setIssue(null)} />}
    </div>
  );
}
