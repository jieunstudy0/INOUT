import { useState, useEffect, useCallback } from 'react';
import { getMyDepositHistory } from '../api/depositEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const btnBase = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';

  return (
    <div className="flex items-center gap-1 mt-6">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className={`${btnBase} ${page === 0 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>&lt;</button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)} className={`${btnBase} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>{p + 1}</button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className={`${btnBase} ${page >= totalPages - 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>&gt;</button>
    </div>
  );
}

export default function DepositEmpPage() {
  const [balance, setBalance] = useState(0);
  const [histories, setHistories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadDeposit = useCallback((pg) => {
    setLoading(true);
    getMyDepositHistory(pg, PAGE_SIZE)
      .then((data) => {
        setBalance(data?.currentBalance ?? 0);
        setHistories(data?.histories?.content || []);
        setTotalPages(data?.histories?.totalPages || 0);
      })
      .catch(() => Toast.error('예치금 내역을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadDeposit(page);
  }, [page, loadDeposit]);

  const getTypeBadge = (type) => {
    switch (type) {
      case 'CHARGE': return <span className="px-2.5 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-lg">충전</span>;
      case 'REFUND': return <span className="px-2.5 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded-lg">환불</span>;
      case 'PAYMENT': return <span className="px-2.5 py-1 bg-rose-100 text-rose-700 text-xs font-bold rounded-lg">결제</span>;
      default: return <span className="px-2.5 py-1 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg">{type}</span>;
    }
  };

  const getAmountDisplay = (type, amount) => {
    const isPlus = type === 'CHARGE' || type === 'REFUND';
    return (
      <span className={`font-bold ${isPlus ? 'text-emerald-600' : 'text-rose-500'}`}>
        {isPlus ? '+' : '-'}{amount.toLocaleString()}원
      </span>
    );
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">매장 예치금</h2>
        <p className="text-sm text-slate-500 mt-0.5">
          매장 예치금 잔액과 결제·충전 이력을 확인합니다. 충전은 점주가 신청하고 본사가 승인한 뒤 반영됩니다.
        </p>
      </div>

      <div className="bg-gradient-to-r from-slate-800 to-slate-900 rounded-2xl shadow-lg p-8 relative overflow-hidden">
        <div className="absolute -right-6 -top-6 w-32 h-32 bg-white opacity-5 rounded-full blur-2xl" />
        <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">사용 가능한 예치금 잔액</p>
            <div className="flex items-baseline gap-2">
              <span className="text-4xl font-extrabold text-white tracking-tight">
                {Number(balance).toLocaleString('ko-KR')}
              </span>
              <span className="text-lg font-medium text-slate-300">원</span>
            </div>
            <p className="text-xs text-slate-400 mt-3">
              잔액이 부족하면 점주에게 충전 신청을 요청해 주세요. 발주 결제는 장바구니·발주 메뉴에서 진행합니다.
            </p>
          </div>
          <div className="w-16 h-16 bg-white/10 rounded-full items-center justify-center backdrop-blur-sm border border-white/10 hidden sm:flex">
            <svg className="w-8 h-8 text-emerald-400" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a2.25 2.25 0 00-2.25-2.25H15a3 3 0 11-6 0H5.25A2.25 2.25 0 003 12m18 0v6a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18v-6m18 0V9M3 12V9m18 0a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 9m18 0V6a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 6v3" />
            </svg>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
          <h3 className="font-bold text-slate-800">거래 내역</h3>
          <button onClick={() => loadDeposit(page)} className="text-slate-400 hover:text-indigo-600 transition-colors p-1 rounded hover:bg-slate-100">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
          </button>
        </div>

        {loading && histories.length === 0 ? (
          <div className="flex justify-center py-20"><Spinner size="lg" /></div>
        ) : histories.length === 0 ? (
          <EmptyState message="예치금 거래 내역이 없습니다." />
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-[11px] font-semibold text-slate-500 w-40">거래 일시</th>
                  <th className="px-6 py-3 text-center text-[11px] font-semibold text-slate-500 w-24">구분</th>
                  <th className="px-6 py-3 text-left text-[11px] font-semibold text-slate-500">상세 내용</th>
                  <th className="px-6 py-3 text-right text-[11px] font-semibold text-slate-500 w-32">금액</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {histories.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 text-sm text-slate-500 whitespace-nowrap">
                      {new Date(item.createdAt).toLocaleString('ko-KR', {
                        year: 'numeric', month: '2-digit', day: '2-digit',
                        hour: '2-digit', minute: '2-digit',
                      })}
                    </td>
                    <td className="px-6 py-4 text-center">{getTypeBadge(item.type)}</td>
                    <td className="px-6 py-4 text-sm text-slate-800 font-medium">{item.description || '-'}</td>
                    <td className="px-6 py-4 text-right text-sm">{getAmountDisplay(item.type, item.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="flex justify-center">
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </div>
  );
}
