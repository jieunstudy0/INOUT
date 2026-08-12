import { useEffect, useState } from 'react';
import { trackDelivery } from '../../api/deliveryApi';
import Spinner from '../common/Spinner';

/**
 * 배송 타임라인 모달 — GET /api/deliveries/tracking 결과 표시
 */
export default function DeliveryTrackingModal({ open, onClose, carrier, trackingNumber }) {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || !trackingNumber) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');
    setData(null);
    trackDelivery({ carrier, trackingNumber })
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) setError(err?.message || '배송 조회에 실패했습니다.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [open, carrier, trackingNumber]);

  if (!open) return null;

  const formatTime = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return Number.isNaN(d.getTime())
      ? String(val)
      : d.toLocaleString('ko-KR', {
        month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
      });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-xl overflow-hidden max-h-[90vh] flex flex-col">
        <div className="px-6 py-5 border-b border-slate-100 flex items-start justify-between gap-3">
          <div>
            <h3 className="text-lg font-bold text-slate-800">배송 조회</h3>
            <p className="text-xs text-slate-500 mt-1">
              {carrier || 'CJ대한통운'} · <span className="font-mono">{trackingNumber}</span>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100"
            aria-label="닫기"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1">
          {loading && (
            <div className="flex flex-col items-center justify-center py-12 gap-3 text-slate-400">
              <Spinner size="lg" />
              <p className="text-sm">배송 정보를 조회하는 중...</p>
            </div>
          )}

          {!loading && error && (
            <p className="text-sm text-rose-600 text-center py-8">{error}</p>
          )}

          {!loading && data && (
            <>
              <div className="flex items-center justify-between mb-5 gap-2 flex-wrap">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  {data.currentStatus || '조회 완료'}
                </span>
                {data.mockFallback && (
                  <span className="text-[10px] font-medium text-amber-600 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                    시연용 Mock 타임라인
                  </span>
                )}
              </div>

              <ol className="relative border-l-2 border-indigo-100 ml-3 space-y-5">
                {(data.events || []).map((ev, idx) => (
                  <li key={`${ev.time}-${idx}`} className="ml-4">
                    <span className="absolute -left-[7px] mt-1.5 w-3 h-3 rounded-full bg-indigo-500 ring-4 ring-white" />
                    <p className="text-[11px] text-slate-400 font-medium">{formatTime(ev.time)}</p>
                    <p className="text-sm font-bold text-slate-800 mt-0.5">{ev.status}</p>
                    <p className="text-xs text-slate-600 mt-0.5">{ev.location}</p>
                    {ev.description && (
                      <p className="text-xs text-slate-500 mt-1">{ev.description}</p>
                    )}
                  </li>
                ))}
              </ol>
            </>
          )}
        </div>

        <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 text-sm font-semibold text-slate-700 bg-white border border-slate-200 rounded-xl hover:bg-slate-100"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
