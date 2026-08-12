import { useState, useEffect, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  getOwnerDepositHistory,
  requestOwnerCharge,
  getOwnerChargeRequests,
} from '../api/depositOwnerApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { dispatchHeaderRefresh } from '../utils/headerSync';

const PAGE_SIZE = 10;

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
  const pages = Array.from({ length: end - start }, (_, i) => start + i);
  const base = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';
  return (
    <div className="flex items-center gap-1 mt-6">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${base} ${page === 0 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${base} ${p === page ? 'bg-emerald-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${base} ${page >= totalPages - 1 ? 'text-slate-300' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

function ChargeModal({ open, onClose, onSuccess }) {
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  if (!open) return null;
  const raw = Number(String(amount).replace(/,/g, '')) || 0;

  const submit = async () => {
    if (raw <= 0) {
      Toast.error('충전 신청 금액을 입력해 주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await requestOwnerCharge({ amount: raw });
      Toast.success('예치금 충전 신청이 완료되었습니다.');
      setAmount('');
      onSuccess?.();
      onClose();
    } catch {
      /* toast */
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <h3 className="text-lg font-bold text-slate-800 mb-1">예치금 충전 신청</h3>
        <p className="text-sm text-slate-500 mb-4">본사 승인 후 매장 예치금에 반영됩니다.</p>
        <input
          type="text"
          inputMode="numeric"
          value={amount}
          onChange={(e) => {
            const digits = e.target.value.replace(/[^0-9]/g, '');
            setAmount(digits ? Number(digits).toLocaleString('ko-KR') : '');
          }}
          placeholder="신청 금액"
          className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-400/40"
        />
        <div className="flex gap-2 mt-5">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl">취소</button>
          <button type="button" onClick={submit} disabled={submitting} className="flex-1 py-2.5 text-sm font-bold bg-emerald-600 text-white rounded-xl disabled:opacity-50">
            {submitting ? '신청 중...' : '충전 신청'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function OwnerDepositManagement() {
  const { refreshSummary } = useOutletContext() || {};
  const [balance, setBalance] = useState(0);
  const [histories, setHistories] = useState([]);
  const [charges, setCharges] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [chargeOpen, setChargeOpen] = useState(false);

  const load = useCallback((pg) => {
    setLoading(true);
    Promise.all([
      getOwnerDepositHistory(pg, PAGE_SIZE),
      getOwnerChargeRequests(),
    ])
      .then(([deposit, chargeList]) => {
        setBalance(deposit?.currentBalance ?? 0);
        setHistories(deposit?.histories?.content || []);
        setTotalPages(deposit?.histories?.totalPages || 0);
        setCharges(Array.isArray(chargeList) ? chargeList : []);
      })
      .catch(() => Toast.error('예치금 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page); }, [page, load]);

  const handleChargeSuccess = () => {
    setPage(0);
    load(0);
    refreshSummary?.();
    dispatchHeaderRefresh({ role: 'OWNER' });
  };

  const typeBadge = (type) => {
    if (type === 'CHARGE') return <span className="px-2.5 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-lg">충전</span>;
    if (type === 'REFUND') return <span className="px-2.5 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded-lg">환불</span>;
    if (type === 'PAYMENT') return <span className="px-2.5 py-1 bg-rose-100 text-rose-700 text-xs font-bold rounded-lg">결제</span>;
    return <span className="px-2.5 py-1 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg">{type}</span>;
  };

  const chargeStatusBadge = (status) => {
    if (status === 'PENDING') return <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">승인 대기</span>;
    if (status === 'APPROVED') return <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 border border-emerald-200">승인 완료</span>;
    if (status === 'REJECTED') return <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 border border-rose-200">반려</span>;
    return <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">{status}</span>;
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <h2 className="text-xl font-bold text-slate-800">예치금/결제 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            매장 예치금 잔액·이력을 확인하고, 본사 승인이 필요한 충전을 신청합니다.
          </p>
        </div>
        <button
          onClick={() => setChargeOpen(true)}
          className="px-4 py-2.5 text-sm font-bold rounded-xl bg-emerald-600 text-white hover:bg-emerald-700 shadow-sm"
        >
          충전 신청
        </button>
      </div>

      <div className="bg-gradient-to-r from-slate-800 to-slate-900 rounded-2xl shadow-lg p-8 relative overflow-hidden">
        <p className="text-slate-400 text-sm font-medium mb-1">매장 예치금 잔액</p>
        <div className="flex items-baseline gap-2">
          <span className="text-4xl font-extrabold text-white tracking-tight">{Number(balance).toLocaleString('ko-KR')}</span>
          <span className="text-lg font-medium text-slate-300">원</span>
        </div>
      </div>

      <section className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="text-sm font-bold text-slate-800">거래 내역</h3>
        </div>
        {loading ? (
          <div className="p-10 flex justify-center"><Spinner /></div>
        ) : histories.length === 0 ? (
          <div className="p-6"><EmptyState title="거래 내역이 없습니다." /></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 text-xs text-slate-500">
                <tr>
                  <th className="px-4 py-2.5 text-left font-semibold">유형</th>
                  <th className="px-4 py-2.5 text-left font-semibold">설명</th>
                  <th className="px-4 py-2.5 text-right font-semibold">금액</th>
                  <th className="px-4 py-2.5 text-right font-semibold">일시</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {histories.map((h) => {
                  const isPlus = h.type === 'CHARGE' || h.type === 'REFUND';
                  return (
                    <tr key={h.id}>
                      <td className="px-4 py-3">{typeBadge(h.type)}</td>
                      <td className="px-4 py-3 text-slate-600">{h.description || '-'}</td>
                      <td className={`px-4 py-3 text-right font-bold tabular-nums ${isPlus ? 'text-emerald-600' : 'text-rose-500'}`}>
                        {isPlus ? '+' : '-'}{Number(h.amount || 0).toLocaleString('ko-KR')}원
                      </td>
                      <td className="px-4 py-3 text-right text-slate-500 text-xs">
                        {h.createdAt ? new Date(h.createdAt).toLocaleString('ko-KR') : '-'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <div className="px-5 pb-5">
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </div>
      </section>

      <section className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="text-sm font-bold text-slate-800">충전 신청 내역</h3>
        </div>
        {charges.length === 0 ? (
          <div className="p-6"><EmptyState title="충전 신청 내역이 없습니다." /></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 text-xs text-slate-500">
                <tr>
                  <th className="px-4 py-2.5 text-left font-semibold">신청번호</th>
                  <th className="px-4 py-2.5 text-right font-semibold">금액</th>
                  <th className="px-4 py-2.5 text-left font-semibold">상태</th>
                  <th className="px-4 py-2.5 text-right font-semibold">신청일</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {charges.map((c) => (
                  <tr key={c.id}>
                    <td className="px-4 py-3 font-semibold text-slate-800">#{c.id}</td>
                    <td className="px-4 py-3 text-right font-bold tabular-nums">{Number(c.amount || 0).toLocaleString('ko-KR')}원</td>
                    <td className="px-4 py-3">{chargeStatusBadge(c.status)}</td>
                    <td className="px-4 py-3 text-right text-xs text-slate-500">{c.requestDate ? new Date(c.requestDate).toLocaleString('ko-KR') : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <ChargeModal open={chargeOpen} onClose={() => setChargeOpen(false)} onSuccess={handleChargeSuccess} />
    </div>
  );
}
