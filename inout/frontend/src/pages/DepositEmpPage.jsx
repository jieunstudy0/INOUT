import { useState, useEffect, useCallback } from 'react';
import { getMyDepositHistory, chargeMyDeposit } from '../api/depositEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;


function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end   = Math.min(totalPages, start + 5);
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
  const [isChargeModalOpen, setIsChargeModalOpen] = useState(false);
  const [chargeAmount, setChargeAmount] = useState('');
  const [isCharging, setIsCharging] = useState(false);

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

  const handleAmountChange = (e) => {
    const value = e.target.value.replace(/[^0-9]/g, '');
    setChargeAmount(value ? Number(value).toLocaleString() : '');
  };

  const handleAddAmount = (addValue) => {
    const current = Number(chargeAmount.replace(/,/g, '')) || 0;
    setChargeAmount((current + addValue).toLocaleString());
  };
  
  const handleChargeSubmit = async () => {
    const amount = Number(chargeAmount.replace(/,/g, ''));
    if (!amount || amount <= 0) {
      Toast.error('충전할 금액을 올바르게 입력해주세요.');
      return;
    }

    setIsCharging(true);
    try {
      await chargeMyDeposit({ 
        amount: amount, 
        description: '가맹점 직접 충전' 
      });
      Toast.success(`${amount.toLocaleString()}원이 충전되었습니다.`);
      setIsChargeModalOpen(false);
      setChargeAmount('');
      setPage(0); 
      loadDeposit(0); 
    } catch (err) {
      Toast.error('충전에 실패했습니다.');
    } finally {
      setIsCharging(false);
    }
  };

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
        <h2 className="text-xl font-bold text-slate-800">나의 예치금</h2>
        <p className="text-sm text-slate-500 mt-0.5">매장의 예치금 잔액과 결제 및 충전 내역을 확인합니다.</p>
      </div>

      {/* ── 상단 예치금 잔액 요약 카드 ── */}
      <div className="bg-gradient-to-r from-slate-800 to-slate-900 rounded-2xl shadow-lg p-8 relative overflow-hidden">
        <div className="absolute -right-6 -top-6 w-32 h-32 bg-white opacity-5 rounded-full blur-2xl" />
        <div className="absolute -right-10 -bottom-10 w-40 h-40 bg-indigo-400 opacity-10 rounded-full blur-2xl" />
        
        <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <p className="text-slate-400 text-sm font-medium mb-1">사용 가능한 예치금 잔액</p>
            <div className="flex items-baseline gap-2">
              <span className="text-4xl font-extrabold text-white tracking-tight">
                {balance.toLocaleString()}
              </span>
              <span className="text-lg font-medium text-slate-300">원</span>
            </div>
          </div>
          
          {/* 충전하기 버튼 추가 */}
          <div className="flex items-center gap-4 w-full sm:w-auto">
            <button 
              onClick={() => setIsChargeModalOpen(true)}
              className="flex-1 sm:flex-none px-6 py-3.5 bg-white text-slate-900 font-bold text-sm rounded-xl hover:bg-slate-100 transition-colors shadow-sm flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              충전하기
            </button>
            <div className="w-16 h-16 bg-white/10 rounded-full items-center justify-center backdrop-blur-sm border border-white/10 hidden sm:flex">
              <svg className="w-8 h-8 text-emerald-400" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a2.25 2.25 0 00-2.25-2.25H15a3 3 0 11-6 0H5.25A2.25 2.25 0 003 12m18 0v6a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18v-6m18 0V9M3 12V9m18 0a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 9m18 0V6a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 6v3" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* ── 최근 거래 내역 테이블 ── */}
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
                        hour: '2-digit', minute: '2-digit'
                      })}
                    </td>
                    <td className="px-6 py-4 text-center">
                      {getTypeBadge(item.type)}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-800 font-medium">
                      {item.description || '-'}
                    </td>
                    <td className="px-6 py-4 text-right text-sm">
                      {getAmountDisplay(item.type, item.amount)}
                    </td>
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

      {/* ── 예치금 충전 모달 ── */}
      {isChargeModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-5 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-800">예치금 충전</h3>
            </div>
            
            <div className="p-6 space-y-6">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">충전 금액 (원)</label>
                <input
                  type="text"
                  value={chargeAmount}
                  onChange={handleAmountChange}
                  placeholder="0"
                  className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-right text-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                />
              </div>

              <div className="grid grid-cols-3 gap-2">
                <button onClick={() => handleAddAmount(10000)} className="py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 active:bg-slate-100">+ 1만</button>
                <button onClick={() => handleAddAmount(50000)} className="py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 active:bg-slate-100">+ 5만</button>
                <button onClick={() => handleAddAmount(100000)} className="py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 active:bg-slate-100">+ 10만</button>
              </div>
            </div>

            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-2">
              <button 
                onClick={() => {
                  setIsChargeModalOpen(false);
                  setChargeAmount('');
                }} 
                className="px-5 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-200 rounded-xl transition-colors"
                disabled={isCharging}
              >
                취소
              </button>
              <button 
                onClick={handleChargeSubmit}
                disabled={isCharging || !chargeAmount}
                className="px-6 py-2.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 disabled:opacity-50 transition-colors flex items-center gap-2"
              >
                {isCharging ? <Spinner size="sm" /> : '충전하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}