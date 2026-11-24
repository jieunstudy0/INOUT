import { useState, useEffect, useCallback, useRef } from 'react';
import {
  getStockCountSheets,
  createStockCountSheet,
  getStockCountSheet,
  scanStockCountPhoto,
  submitStockCount,
} from '../api/stockCountApi';
import { Toast } from '../utils/toast';
import { isStaleDataError } from '../utils/resultCodes';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import StockCountReceipt from '../components/stock/StockCountReceipt';
import StatusPill from '../components/stock/StockCountStatusPill';
import { REASON_TEXT, formatDateTime } from '../utils/stockCount';
import { confirmDialog } from '../utils/dialog';

/** 입력칸 값(문자열) → 제출 값. 빈칸은 null(세지 않음). */
function toQty(value) {
  if (value === '' || value === null || value === undefined) return null;
  return Number(value);
}

export default function StockCountEmpPage() {
  const [sheets, setSheets] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [sheet, setSheet] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [counts, setCounts] = useState({});
  const [creating, setCreating] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef(null);

  const loadList = useCallback(() => {
    setListLoading(true);
    return getStockCountSheets()
      .then((data) => {
        const list = data || [];
        setSheets(list);
        // 처음 들어오면 가장 최근 작성 중인 실사표를 연다.
        setSelectedId((prev) => prev ?? (list.find((s) => s.status === 'OPEN') || list[0])?.sheetId ?? null);
      })
      .catch(() => {})
      .finally(() => setListLoading(false));
  }, []);

  const applySheet = (data) => {
    setSheet(data);
    const next = {};
    data.lines.forEach((l) => {
      const v = l.countedQty ?? l.aiCountedQty;
      next[l.lineId] = v ?? '';
    });
    setCounts(next);
  };

  const loadDetail = useCallback((id) => {
    if (!id) { setSheet(null); return; }
    setDetailLoading(true);
    getStockCountSheet(id)
      .then(applySheet)
      .catch(() => {})
      .finally(() => setDetailLoading(false));
  }, []);

  useEffect(() => { loadList(); }, [loadList]);
  useEffect(() => { loadDetail(selectedId); }, [selectedId, loadDetail]);

  const handleCreate = async () => {
    setCreating(true);
    try {
      const id = await createStockCountSheet();
      Toast.success('실사표를 만들었습니다. 영수증을 출력해 주세요.');
      setSelectedId(id);
      loadList();
    } catch {
      /* interceptor */
    } finally {
      setCreating(false);
    }
  };

  const handlePhoto = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !sheet) return;
    setScanning(true);
    try {
      const data = await scanStockCountPhoto(sheet.sheetId, file);
      applySheet(data);
      const read = data.lines.filter((l) => l.aiCountedQty != null).length;
      const unsure = data.lines.filter((l) => l.aiCountedQty != null && !l.aiConfident).length;
      Toast.success(`AI가 ${data.lines.length}품목 중 ${read}품목을 읽었습니다.${unsure ? ` 노란 칸 ${unsure}개는 꼭 확인해 주세요.` : ''}`);
    } catch {
      /* interceptor — AI 실패 시에도 직접 입력해서 제출할 수 있다 */
    } finally {
      setScanning(false);
    }
  };

  const handleSubmit = async () => {
    const lines = sheet.lines.map((l) => ({ lineId: l.lineId, countedQty: toQty(counts[l.lineId]) }));
    const counted = lines.filter((l) => l.countedQty !== null).length;
    if (counted === 0) {
      Toast.warning('실재고를 한 개 이상 입력해 주세요.');
      return;
    }
    const diffCount = sheet.lines.filter((l) => {
      const q = toQty(counts[l.lineId]);
      return q !== null && q !== l.systemQty;
    }).length;
    const blank = lines.length - counted;
    const message = `${counted}품목을 제출합니다. (차이 ${diffCount}품목${blank ? `, 비워 둔 ${blank}품목은 반영되지 않음` : ''})\n점주가 승인하면 매장 재고에 반영됩니다.`;
    if (!await confirmDialog(message)) return;

    setSubmitting(true);
    try {
      await submitStockCount(sheet.sheetId, lines);
      Toast.success('실재고를 제출했습니다. 점주 승인 후 반영됩니다.');
      loadDetail(sheet.sheetId);
      loadList();
    } catch (err) {
      // 다른 직원이 먼저 제출했으면 최신 상태로 다시 불러온다.
      if (isStaleDataError(err)) { loadDetail(sheet.sheetId); loadList(); }
    } finally {
      setSubmitting(false);
    }
  };

  const editable = sheet?.status === 'OPEN';

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">재고 실사</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            매일 9시에 우리 매장 실사표가 준비됩니다. 영수증을 출력해 실재고를 적고, 사진을 올리면 AI가 숫자를 채워 줍니다.
          </p>
        </div>
        <button onClick={handleCreate} disabled={creating}
          className="px-4 py-2.5 text-sm font-semibold bg-slate-800 text-white rounded-xl hover:bg-slate-900 disabled:opacity-50">
          {creating ? '만드는 중...' : '지금 실사표 만들기'}
        </button>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[260px_1fr] gap-5">
        {/* 실사표 목록 */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden self-start">
          <div className="px-4 py-3 border-b border-slate-100 text-xs font-semibold text-slate-500">최근 실사표</div>
          {listLoading ? (
            <div className="flex justify-center py-10"><Spinner /></div>
          ) : sheets.length === 0 ? (
            <div className="p-4 text-sm text-slate-400">아직 실사표가 없습니다.</div>
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
                      {formatDateTime(s.createdAt)} · {s.countedCount}/{s.lineCount}품목
                      {s.status === 'OPEN' && s.rejectReason ? <span className="ml-1 text-rose-600">반려됨</span> : null}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* 실사표 상세 */}
        <div className="space-y-4 min-w-0">
          {detailLoading ? (
            <div className="flex justify-center py-20"><Spinner size="lg" /></div>
          ) : !sheet ? (
            <EmptyState message="실사표를 선택하거나 새로 만들어 주세요." />
          ) : (
            <>
              {editable && sheet.rejectReason && (
                <div className="px-4 py-3 rounded-xl border border-rose-200 bg-rose-50 text-sm text-rose-700">
                  <b>점주 반려:</b> {sheet.rejectReason} — 다시 세어 제출해 주세요.
                </div>
              )}
              {sheet.sheetCodeMatched === false && (
                <div className="px-4 py-3 rounded-xl border border-rose-200 bg-rose-50 text-sm text-rose-700">
                  사진 속 영수증 번호가 <b className="font-mono">{sheet.sheetCode}</b>와 다릅니다. 다른 날 영수증을 찍은 건 아닌지 확인해 주세요.
                </div>
              )}

              <div className="grid grid-cols-1 2xl:grid-cols-[1fr_auto] gap-4">
                <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden min-w-0">
                  <div className="flex flex-wrap items-center justify-between gap-2 px-5 py-3 border-b border-slate-100">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-slate-800">{sheet.sheetCode}</span>
                      <StatusPill status={sheet.status} />
                    </div>
                    {editable && (
                      <div className="flex gap-2">
                        <button onClick={() => window.print()}
                          className="px-3 py-2 text-xs font-semibold rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50">
                          영수증 출력
                        </button>
                        <button onClick={() => fileInputRef.current?.click()} disabled={scanning}
                          className="px-3 py-2 text-xs font-semibold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50">
                          {scanning ? 'AI가 읽는 중...' : '사진 올려서 채우기'}
                        </button>
                        <input ref={fileInputRef} type="file" accept="image/jpeg,image/png,image/webp" capture="environment"
                          className="hidden" onChange={handlePhoto} />
                      </div>
                    )}
                  </div>

                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200">
                      <thead className="bg-slate-50">
                        <tr className="text-[11px] font-semibold text-slate-500">
                          <th className="px-3 py-2.5 text-center w-12">No</th>
                          <th className="px-3 py-2.5 text-left">상품명</th>
                          <th className="px-3 py-2.5 text-center w-16">위치</th>
                          <th className="px-3 py-2.5 text-right w-16">기록</th>
                          <th className="px-3 py-2.5 text-center w-16">AI 읽음</th>
                          <th className="px-3 py-2.5 text-center w-24">실재고</th>
                          <th className="px-3 py-2.5 text-right w-14">차이</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {sheet.lines.map((l) => {
                          const value = editable ? counts[l.lineId] : (l.countedQty ?? '');
                          const qty = toQty(value);
                          const diff = qty === null ? null : qty - l.systemQty;
                          const unsure = l.aiCountedQty != null && !l.aiConfident;
                          const notRead = sheet.scannedAt && l.aiCountedQty == null;
                          return (
                            <tr key={l.lineId} className={unsure ? 'bg-amber-50/70' : ''}>
                              <td className="px-3 py-2.5 text-center text-sm font-mono text-slate-500">{String(l.lineNo).padStart(2, '0')}</td>
                              <td className="px-3 py-2.5">
                                <div className="text-sm font-medium text-slate-800">{l.itemName}</div>
                                <div className="text-[11px] text-slate-400">{REASON_TEXT[l.reason]}{l.unitDescription ? ` · ${l.unitDescription}` : ''}</div>
                              </td>
                              <td className="px-3 py-2.5 text-center text-sm text-slate-600">{l.location || '-'}</td>
                              <td className="px-3 py-2.5 text-right text-sm text-slate-700">{l.systemQty}</td>
                              <td className="px-3 py-2.5 text-center text-xs">
                                {l.aiCountedQty != null ? (
                                  <span className={unsure ? 'text-amber-700 font-semibold' : 'text-slate-600'}>
                                    {l.aiCountedQty}{unsure ? ' (?)' : ''}
                                  </span>
                                ) : notRead ? <span className="text-slate-400">못 읽음</span> : '-'}
                              </td>
                              <td className="px-3 py-2.5 text-center">
                                {editable ? (
                                  <input type="number" min="0" max="99999" inputMode="numeric" value={value}
                                    onChange={(e) => setCounts((prev) => ({ ...prev, [l.lineId]: e.target.value }))}
                                    placeholder="안 셈"
                                    className={`w-16 px-2 py-1.5 text-sm text-right border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/40 ${unsure ? 'border-amber-400 bg-white' : 'border-slate-300'}`} />
                                ) : (
                                  <span className="text-sm font-semibold text-slate-800">{l.countedQty ?? '-'}</span>
                                )}
                              </td>
                              <td className={`px-3 py-2.5 text-right text-sm font-semibold ${diff > 0 ? 'text-emerald-600' : diff < 0 ? 'text-rose-600' : 'text-slate-400'}`}>
                                {diff === null ? '' : diff > 0 ? `+${diff}` : diff}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  {editable && (
                    <div className="flex items-center justify-between gap-3 px-5 py-3 border-t border-slate-100 bg-slate-50/60">
                      <p className="text-xs text-slate-500">
                        AI가 읽은 숫자는 참고용입니다. 노란 줄은 꼭 영수증과 비교해 주세요. 비워 둔 상품은 반영되지 않습니다.
                      </p>
                      <button onClick={handleSubmit} disabled={submitting}
                        className="shrink-0 px-5 py-2.5 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50">
                        {submitting ? '제출 중...' : '실재고 제출'}
                      </button>
                    </div>
                  )}
                  {sheet.status !== 'OPEN' && (
                    <div className="px-5 py-3 border-t border-slate-100 text-xs text-slate-500">
                      {sheet.submittedByName} · {formatDateTime(sheet.submittedAt)} 제출
                      {sheet.status === 'APPLIED' && ` → ${sheet.reviewedByName} · ${formatDateTime(sheet.reviewedAt)} 반영`}
                    </div>
                  )}
                </div>

                {/* 영수증 미리보기 (인쇄 시 이 영역만 출력) */}
                {editable && (
                  <div className="self-start">
                    <div className="text-xs font-semibold text-slate-500 mb-2">영수증 미리보기 (80mm)</div>
                    <div className="print-area inline-block bg-white shadow-sm border border-slate-200 px-[4mm]">
                      <StockCountReceipt sheet={sheet} />
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
