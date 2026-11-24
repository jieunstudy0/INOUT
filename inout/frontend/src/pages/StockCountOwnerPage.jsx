import AuthImage from '../components/common/AuthImage';
import { useState, useEffect, useCallback } from 'react';
import {
  getOwnerStockCountSheets,
  getOwnerStockCountSheet,
  approveStockCount,
  rejectStockCount,
} from '../api/stockCountApi';
import { Toast } from '../utils/toast';
import { isStaleDataError } from '../utils/resultCodes';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import StatusPill from '../components/stock/StockCountStatusPill';
import { REASON_TEXT, formatDateTime } from '../utils/stockCount';

const FILTERS = [
  { value: 'SUBMITTED', label: '승인 대기' },
  { value: '', label: '전체' },
];

function signed(n) {
  if (n === null || n === undefined) return '';
  return n > 0 ? `+${n}` : String(n);
}

export default function StockCountOwnerPage() {
  const [filter, setFilter] = useState('SUBMITTED');
  const [sheets, setSheets] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [sheet, setSheet] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [showPhoto, setShowPhoto] = useState(false);

  const loadList = useCallback(() => {
    setListLoading(true);
    getOwnerStockCountSheets(filter)
      .then((data) => setSheets(data || []))
      .catch(() => {})
      .finally(() => setListLoading(false));
  }, [filter]);

  const loadDetail = useCallback((id) => {
    if (!id) { setSheet(null); return; }
    setDetailLoading(true);
    getOwnerStockCountSheet(id)
      .then(setSheet)
      .catch(() => {})
      .finally(() => setDetailLoading(false));
  }, []);

  useEffect(() => { loadList(); }, [loadList]);
  useEffect(() => { loadDetail(selectedId); }, [selectedId, loadDetail]);

  const refresh = () => { loadList(); loadDetail(selectedId); };

  const handleApprove = async () => {
    const changed = sheet.lines.filter((l) => l.diff !== null && l.diff !== 0);
    const msg = changed.length === 0
      ? '기록과 실재고가 모두 같습니다. 승인하시겠습니까?'
      : `${changed.length}품목의 재고가 바뀝니다.\n` +
        changed.slice(0, 8).map((l) => `· ${l.itemName}: ${l.currentStock} → ${l.expectedAfterApply} (${signed(l.diff)})`).join('\n') +
        (changed.length > 8 ? `\n… 외 ${changed.length - 8}품목` : '') +
        '\n\n승인하시겠습니까?';
    if (!window.confirm(msg)) return;

    setProcessing(true);
    try {
      const result = await approveStockCount(sheet.sheetId);
      Toast.success(`재고에 반영했습니다. 증가 ${result.increasedCount} · 감소 ${result.decreasedCount} · 일치 ${result.unchangedCount}품목`);
      refresh();
    } catch (err) {
      if (isStaleDataError(err)) refresh();
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    const reason = rejectReason;
    if (!reason.trim()) {
      Toast.warning('반려 사유를 입력해 주세요.');
      return;
    }
    setProcessing(true);
    try {
      await rejectStockCount(sheet.sheetId, reason.trim());
      Toast.success('반려했습니다. 직원이 다시 세어 제출할 수 있습니다.');
      setRejectReason('');
      refresh();
    } catch {
      /* interceptor */
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">매장 재고 실사 승인</h2>
        <p className="text-sm text-slate-500 mt-0.5">
          직원이 제출한 실재고를 확인하고 승인하면, 출력 시점 재고와의 차이만큼 지금 매장 재고에 반영됩니다.
        </p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[260px_1fr] gap-5">
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden self-start">
          <div className="flex gap-1 p-2 border-b border-slate-100">
            {FILTERS.map((f) => (
              <button key={f.value} onClick={() => { setFilter(f.value); setSelectedId(null); }}
                className={`flex-1 py-1.5 text-xs font-semibold rounded-lg ${filter === f.value ? 'bg-slate-800 text-white' : 'text-slate-500 hover:bg-slate-100'}`}>
                {f.label}
              </button>
            ))}
          </div>
          {listLoading ? (
            <div className="flex justify-center py-10"><Spinner /></div>
          ) : sheets.length === 0 ? (
            <div className="p-4 text-sm text-slate-400">해당하는 실사표가 없습니다.</div>
          ) : (
            <ul className="divide-y divide-slate-100 max-h-48 xl:max-h-[70vh] overflow-y-auto">
              {sheets.map((s) => (
                <li key={s.sheetId}>
                  <button onClick={() => setSelectedId(s.sheetId)}
                    className={`w-full text-left px-4 py-3 hover:bg-slate-50 ${selectedId === s.sheetId ? 'bg-indigo-50/60' : ''}`}>
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-semibold text-slate-800 font-mono">{s.sheetCode}</span>
                      <StatusPill status={s.status} />
                    </div>
                    <div className="text-xs text-slate-500 mt-1">
                      {s.submittedByName ? `${s.submittedByName} · ${formatDateTime(s.submittedAt)}` : formatDateTime(s.createdAt)}
                      {' · '}차이 {s.mismatchCount}품목
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="min-w-0">
          {detailLoading ? (
            <div className="flex justify-center py-20"><Spinner size="lg" /></div>
          ) : !sheet ? (
            <EmptyState message="왼쪽에서 실사표를 선택해 주세요." />
          ) : (
            <div className="space-y-4">
              {sheet.sheetCodeMatched === false && (
                <div className="px-4 py-3 rounded-xl border border-rose-200 bg-rose-50 text-sm text-rose-700">
                  AI가 읽은 사진 속 영수증 번호가 이 실사표와 다릅니다. 사진을 확인해 주세요.
                </div>
              )}

              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="flex flex-wrap items-center justify-between gap-2 px-5 py-3 border-b border-slate-100">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-800">{sheet.sheetCode}</span>
                    <StatusPill status={sheet.status} />
                    <span className="text-xs text-slate-500">
                      출력 {formatDateTime(sheet.createdAt)}
                      {sheet.submittedByName && ` · 제출 ${sheet.submittedByName} ${formatDateTime(sheet.submittedAt)}`}
                    </span>
                  </div>
                  <div className="flex gap-2">
                    {sheet.photoUrl && (
                      <button type="button" onClick={() => setShowPhoto((v) => !v)}
                        className="px-3 py-2 text-xs font-semibold rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50">
                        {showPhoto ? '사진 닫기' : '영수증 사진 보기'}
                      </button>
                    )}
                    {sheet.status === 'SUBMITTED' && (
                      <>
                        <input type="text" maxLength={255} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)}
                          placeholder="반려 사유 (반려할 때만)"
                          className="w-44 px-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/40" />
                        <button onClick={handleReject} disabled={processing}
                          className="px-3 py-2 text-xs font-semibold rounded-lg border border-rose-200 text-rose-600 hover:bg-rose-50 disabled:opacity-50">
                          반려
                        </button>
                        <button onClick={handleApprove} disabled={processing}
                          className="px-4 py-2 text-xs font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
                          {processing ? '처리 중...' : '승인하고 재고 반영'}
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {showPhoto && sheet.photoUrl && (
                  <div className="px-5 py-4 border-b border-slate-100 bg-slate-50">
                    <AuthImage src={sheet.photoUrl} alt="영수증 사진" className="max-h-[480px] mx-auto rounded-[10px] border border-slate-200" />
                  </div>
                )}
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200">
                    <thead className="bg-slate-50">
                      <tr className="text-[11px] font-semibold text-slate-500">
                        <th className="px-3 py-2.5 text-center w-12">No</th>
                        <th className="px-3 py-2.5 text-left">상품명</th>
                        <th className="px-3 py-2.5 text-center">위치</th>
                        <th className="px-3 py-2.5 text-right" title="영수증을 출력할 때 기록된 매장 재고">출력 시점</th>
                        <th className="px-3 py-2.5 text-right">AI 읽음</th>
                        <th className="px-3 py-2.5 text-right">실재고</th>
                        <th className="px-3 py-2.5 text-right">차이</th>
                        <th className="px-3 py-2.5 text-right" title="출력 이후 수령·사용으로 바뀌었을 수 있습니다">지금 재고</th>
                        <th className="px-3 py-2.5 text-right">
                          {sheet.status === 'APPLIED' ? '반영량' : '승인 후'}
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {sheet.lines.map((l) => {
                        const aiEdited = l.aiCountedQty != null && l.countedQty != null && l.aiCountedQty !== l.countedQty;
                        const negative = l.expectedAfterApply != null && l.expectedAfterApply < 0;
                        return (
                          <tr key={l.lineId} className={negative ? 'bg-rose-50' : ''}>
                            <td className="px-3 py-2.5 text-center text-sm font-mono text-slate-500">{String(l.lineNo).padStart(2, '0')}</td>
                            <td className="px-3 py-2.5">
                              <div className="text-sm font-medium text-slate-800">{l.itemName}</div>
                              <div className="text-[11px] text-slate-400">{REASON_TEXT[l.reason]}</div>
                            </td>
                            <td className="px-3 py-2.5 text-center text-sm text-slate-600">{l.location || '-'}</td>
                            <td className="px-3 py-2.5 text-right text-sm text-slate-600">{l.systemQty}</td>
                            <td className="px-3 py-2.5 text-right text-xs text-slate-500">
                              {l.aiCountedQty ?? '-'}{l.aiCountedQty != null && !l.aiConfident ? ' (?)' : ''}
                            </td>
                            <td className="px-3 py-2.5 text-right text-sm font-semibold text-slate-800">
                              {l.countedQty ?? <span className="text-slate-400 font-normal">안 셈</span>}
                              {aiEdited && <span className="ml-1 text-[10px] text-indigo-600" title="직원이 AI 값을 고침">수정</span>}
                            </td>
                            <td className={`px-3 py-2.5 text-right text-sm font-semibold ${l.diff > 0 ? 'text-emerald-600' : l.diff < 0 ? 'text-rose-600' : 'text-slate-400'}`}>
                              {signed(l.diff)}
                            </td>
                            <td className="px-3 py-2.5 text-right text-sm text-slate-600">{l.currentStock ?? '-'}</td>
                            <td className="px-3 py-2.5 text-right text-sm font-semibold text-slate-800">
                              {sheet.status === 'APPLIED' ? signed(l.appliedDiff) : (l.expectedAfterApply ?? '-')}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                <div className="px-5 py-3 border-t border-slate-100 text-xs text-slate-500">
                  승인하면 줄마다 <b>(실재고 − 출력 시점 재고)</b>만큼 지금 재고를 늘리거나 줄입니다. 출력 이후의 수령·사용 기록은 그대로 유지됩니다.
                  빨간 줄은 반영하면 0보다 작아지는 상품이라 승인되지 않습니다.
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
