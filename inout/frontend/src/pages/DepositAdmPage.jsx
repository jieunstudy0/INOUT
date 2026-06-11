import { useState, useEffect, useCallback } from 'react';
import { getAdminDepositList, adminChargeDeposit, getFranchiseeUserList } from '../api/adminDepositApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

function TransactionTypeBadge({ type }) {
  const config = {
    CHARGE: { label: '충전 (+)', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    USE:    { label: '사용 (-)', cls: 'bg-rose-100 text-rose-700 border-rose-200' },
    REFUND: { label: '환불 (-)', cls: 'bg-slate-100 text-slate-600 border-slate-200' },
    PAYMENT: { label: '결제 (-)', cls: 'bg-rose-100 text-rose-700 border-rose-200' },
  }[type] || { label: type, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${config.cls}`}>{config.label}</span>;
}

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

export default function DepositAdmPage() {
  const [histories, setHistories] = useState([]);
  const [summary, setSummary] = useState({ totalBalance: 0, monthlyCharge: 0, monthlyUsage: 0 });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filters, setFilters] = useState({ type: '', keyword: '' });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [franchiseeList, setFranchiseeList] = useState([]); // 가맹점 목록 데이터
  const [isFetchingUsers, setIsFetchingUsers] = useState(false); // 목록 로딩 상태
  const [chargeData, setChargeData] = useState({ targetUserId: '', amount: '', description: '본사 수동 지급' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadData = useCallback((pg) => {
    setLoading(true);
    getAdminDepositList({ page: pg, size: PAGE_SIZE, ...filters })
      .then(data => {
        setHistories(data.histories?.content || []);
        setTotalPages(data.histories?.totalPages || 0);
        if (data.summary) setSummary(data.summary);
      })
      .catch(() => Toast.error('예치금 내역을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadData(page); }, [page, loadData]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadData(0);
  };

  const handleOpenModal = async () => {
    setIsModalOpen(true);
    if (franchiseeList.length === 0) {
      setIsFetchingUsers(true);
      try {
        const users = await getFranchiseeUserList();
        setFranchiseeList(users || []);
      } catch (error) {
        Toast.error('가맹점 목록을 불러오지 못했습니다.');
      } finally {
        setIsFetchingUsers(false);
      }
    }
  };

  const handleAmountChange = (e) => {
    const value = e.target.value.replace(/[^0-9]/g, '');
    setChargeData({ ...chargeData, amount: value ? Number(value).toLocaleString() : '' });
  };

  const handleChargeSubmit = async () => {
    const numericAmount = Number(chargeData.amount.replace(/,/g, ''));
    if (!chargeData.targetUserId || numericAmount <= 0) {
      Toast.error('대상 가맹점과 충전 금액을 정확히 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    try {
      await adminChargeDeposit({
        targetUserId: Number(chargeData.targetUserId),
        amount: numericAmount,
        description: chargeData.description
      });
      Toast.success('수동 충전이 완료되었습니다.');
      setIsModalOpen(false);
      setChargeData({ targetUserId: '', amount: '', description: '본사 수동 지급' });
      setPage(0);
      loadData(0);
    } catch (err) {
      Toast.error('충전에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">가맹점 예치금 현황</h2>
          <p className="text-sm text-slate-500 mt-0.5">전체 매장의 예치금 잔액과 변동 내역을 모니터링합니다.</p>
        </div>
        <button 
          onClick={handleOpenModal} 
          className="px-5 py-2.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 transition-colors shadow-sm flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          가맹점 수동 지급
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white p-6 rounded-2xl border border-indigo-100 shadow-sm flex items-center justify-between relative overflow-hidden">
          <div className="absolute top-0 left-0 w-1.5 h-full bg-indigo-500"></div>
          <div><p className="text-xs font-semibold text-slate-500">총 예치금 잔액 (본사 보관금)</p><p className="text-2xl font-extrabold text-slate-800 mt-1">{summary.totalBalance.toLocaleString()}원</p></div>
          <div className="w-12 h-12 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-500"><svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg></div>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">이번 달 총 충전액</p><p className="text-2xl font-extrabold text-emerald-600 mt-1">+{summary.monthlyCharge.toLocaleString()}원</p></div>
          <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500"><svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" /></svg></div>
        </div>
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">이번 달 발주 사용액</p><p className="text-2xl font-extrabold text-rose-600 mt-1">-{summary.monthlyUsage.toLocaleString()}원</p></div>
          <div className="w-12 h-12 rounded-full bg-rose-50 flex items-center justify-center text-rose-500"><svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z" /></svg></div>
        </div>
      </div>

      <form onSubmit={handleSearch} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap gap-3 items-center">
        <select value={filters.type} onChange={(e) => setFilters({...filters, type: e.target.value})} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[140px]">
          <option value="">모든 거래</option>
          <option value="CHARGE">충전 내역만</option>
          <option value="PAYMENT">결제 내역만</option>
        </select>
        <div className="flex-1 flex min-w-[200px] relative">
          <input 
            type="text" 
            placeholder="가맹점 이름으로 검색" 
            value={filters.keyword} 
            onChange={(e) => setFilters({...filters, keyword: e.target.value})}
            className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        </div>
        <button type="submit" className="px-5 py-2 bg-slate-800 text-white text-sm font-bold rounded-xl hover:bg-slate-900 transition-colors">조회</button>
      </form>

      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) 
      : histories.length === 0 ? ( <EmptyState message="조건에 맞는 예치금 내역이 없습니다." /> ) 
      : (
        <>
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    {['발생 일시', '가맹점명', '구분', '상세 내용', '발생 금액', '거래 후 잔액'].map(h => (
                      <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {histories.map(history => (
                    <tr key={history.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-5 py-4 text-xs text-slate-500">{new Date(history.createdAt).toLocaleString('ko-KR')}</td>
                      <td className="px-5 py-4 text-sm font-bold text-slate-800">{history.storeName}</td>
                      <td className="px-5 py-4"><TransactionTypeBadge type={history.type} /></td>
                      <td className="px-5 py-4 text-sm text-slate-600">{history.description}</td>
                      <td className={`px-5 py-4 text-sm font-bold ${history.type === 'CHARGE' ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {history.type === 'CHARGE' ? '+' : '-'}{history.amount.toLocaleString()}원
                      </td>
                      <td className="px-5 py-4 text-sm font-bold text-slate-700">{history.balanceAfter.toLocaleString()}원</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div className="flex justify-center"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
        </>
      )}

      {/* ──  가맹점 수동 지급 모달 (셀렉트 박스 적용) ── */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-xl overflow-hidden">
            <div className="px-6 py-5 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-800">가맹점 예치금 수동 지급</h3>
            </div>
            
            <div className="p-6 space-y-4">
              {/* 대상 가맹점 셀렉트 박스 */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">대상 가맹점 선택</label>
                {isFetchingUsers ? (
                  <div className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-400 flex items-center gap-2">
                    <Spinner size="sm" /> 가맹점 목록을 불러오는 중...
                  </div>
                ) : (
                  <select
                    value={chargeData.targetUserId}
                    onChange={(e) => setChargeData({...chargeData, targetUserId: e.target.value})}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 cursor-pointer"
                  >
                    <option value="" disabled>가맹점을 선택해주세요</option>
                    {franchiseeList.map((user) => (                
                      <option key={user.userId} value={user.userId}>
                        [{user.storeName}] {user.userName} ({user.email})
                      </option>
                    ))}
                  </select>
                )}
                <p className="text-[11px] text-slate-400 mt-1">* 예치금 계좌가 존재하는 가맹점만 표시됩니다.</p>
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">지급 금액 (원)</label>
                <input
                  type="text"
                  value={chargeData.amount}
                  onChange={handleAmountChange}
                  placeholder="0"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-right font-bold"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">지급 사유 (내역 표기)</label>
                <input
                  type="text"
                  value={chargeData.description}
                  onChange={(e) => setChargeData({...chargeData, description: e.target.value})}
                  placeholder="예: 본사 프로모션 지원금"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-2">
              <button 
                onClick={() => {
                  setIsModalOpen(false);
                  setChargeData({ targetUserId: '', amount: '', description: '본사 수동 지급' });
                }} 
                className="px-5 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-200 rounded-xl transition-colors"
                disabled={isSubmitting}
              >
                취소
              </button>
              <button 
                onClick={handleChargeSubmit}
                disabled={isSubmitting || !chargeData.amount || !chargeData.targetUserId}
                className="px-6 py-2.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 disabled:opacity-50 transition-colors flex items-center gap-2"
              >
                {isSubmitting ? <Spinner size="sm" /> : '지급하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}