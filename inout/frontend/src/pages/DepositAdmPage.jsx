import { useState, useEffect, useCallback } from 'react';
import {
  getAdminDepositList,
  adminChargeDeposit,
  getFranchiseeUserList,
  getPendingChargeRequests,
  approveChargeRequest,
  rejectChargeRequest,
} from '../api/adminDepositApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { dispatchHeaderRefresh } from '../utils/headerSync';

const PAGE_SIZE = 10;

function TransactionTypeBadge({ type }) {
  const config = {
    CHARGE: { label: '충전 (+)', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    USE: { label: '사용 (-)', cls: 'bg-rose-100 text-rose-700 border-rose-200' },
    REFUND: { label: '환불 (-)', cls: 'bg-slate-100 text-slate-600 border-slate-200' },
    PAYMENT: { label: '결제 (-)', cls: 'bg-rose-100 text-rose-700 border-rose-200' },
  }[type] || { label: type, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${config.cls}`}>{config.label}</span>;
}

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

export default function DepositAdmPage() {
  const [mainTab, setMainTab] = useState('pending'); // 'pending' | 'history'

  const [histories, setHistories] = useState([]);
  const [summary, setSummary] = useState({ totalBalance: 0, monthlyCharge: 0, monthlyUsage: 0 });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filters, setFilters] = useState({ type: '', keyword: '' });

  const [pendingCharges, setPendingCharges] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(true);
  const [processingChargeId, setProcessingChargeId] = useState(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [franchiseeList, setFranchiseeList] = useState([]);
  const [isFetchingUsers, setIsFetchingUsers] = useState(false);
  const [chargeData, setChargeData] = useState({ targetUserId: '', amount: '', description: '본사 수동 지급' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadPending = useCallback(() => {
    setPendingLoading(true);
    getPendingChargeRequests()
      .then((data) => setPendingCharges(Array.isArray(data) ? data : []))
      .catch(() => setPendingCharges([]))
      .finally(() => setPendingLoading(false));
  }, []);

  const loadData = useCallback((pg) => {
    setLoading(true);
    getAdminDepositList({ page: pg, size: PAGE_SIZE, ...filters })
      .then((data) => {
        setHistories(data.histories?.content || []);
        setTotalPages(data.histories?.totalPages || 0);
        if (data.summary) setSummary(data.summary);
      })
      .catch(() => Toast.error('예치금 내역을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => { loadPending(); }, [loadPending]);
  useEffect(() => {
    if (mainTab === 'history') loadData(page);
  }, [mainTab, page, loadData]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadData(0);
  };

  const handleApprove = async (chargeId) => {
    if (!window.confirm('이 충전 신청을 승인하고 매장 예치금에 반영할까요?')) return;
    setProcessingChargeId(chargeId);
    try {
      await approveChargeRequest(chargeId);
      Toast.success('충전 신청이 승인되어 예치금이 지급되었습니다.');
      loadPending();
      dispatchHeaderRefresh({ role: 'ADMIN' });
    } catch {
      /* interceptor */
    } finally {
      setProcessingChargeId(null);
    }
  };

  const handleReject = async (chargeId) => {
    const reason = window.prompt('반려 사유를 입력해 주세요.', '본사 검토 후 반려');
    if (reason == null) return;
    setProcessingChargeId(chargeId);
    try {
      await rejectChargeRequest(chargeId, reason.trim() || '본사 검토 후 반려');
      Toast.success('충전 신청이 반려되었습니다.');
      loadPending();
    } catch {
      /* interceptor */
    } finally {
      setProcessingChargeId(null);
    }
  };

  const handleOpenModal = async () => {
    setIsModalOpen(true);
    if (franchiseeList.length === 0) {
      setIsFetchingUsers(true);
      try {
        const users = await getFranchiseeUserList();
        setFranchiseeList(users || []);
      } catch {
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
        description: chargeData.description,
      });
      Toast.success('본사 수동 지급이 완료되었습니다.');
      setIsModalOpen(false);
      setChargeData({ targetUserId: '', amount: '', description: '본사 수동 지급' });
      if (mainTab === 'history') {
        setPage(0);
        loadData(0);
      }
      dispatchHeaderRefresh({ role: 'ADMIN' });
    } catch {
      Toast.error('지급에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">가맹점 예치금 현황</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            점주 충전 신청 승인·본사 수동 지급·거래 이력을 관리합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={handleOpenModal}
          className="px-5 py-2.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 transition-colors shadow-sm flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          가맹점 수동 지급
        </button>
      </div>

      <div className="flex flex-wrap gap-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-1.5 w-fit">
        <button
          type="button"
          onClick={() => setMainTab('pending')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
            mainTab === 'pending' ? 'bg-amber-500 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100'
          }`}
        >
          충전 승인 대기
          <span className={`text-[11px] font-bold px-1.5 py-0.5 rounded-full ${
            mainTab === 'pending' ? 'bg-white/25 text-white' : 'bg-amber-100 text-amber-800'
          }`}
          >
            {pendingCharges.length}
          </span>
        </button>
        <button
          type="button"
          onClick={() => setMainTab('history')}
          className={`px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
            mainTab === 'history' ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:bg-slate-100'
          }`}
        >
          거래 이력 · 수동 지급
        </button>
      </div>

      {mainTab === 'pending' && (
        <section className="bg-white rounded-2xl border border-amber-200 shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-amber-100 bg-amber-50/60 flex items-center justify-between gap-3 flex-wrap">
            <div>
              <h3 className="text-sm font-bold text-amber-900">충전 신청 승인 대기</h3>
              <p className="text-xs text-amber-700/80 mt-0.5">점주가 신청한 PENDING 충전 요청입니다. 입금 확인 후 승인·반려하세요.</p>
            </div>
            <button
              type="button"
              onClick={loadPending}
              className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-white border border-amber-200 text-amber-800 hover:bg-amber-50"
            >
              새로고침
            </button>
          </div>
          {pendingLoading ? (
            <div className="py-10 flex justify-center"><Spinner /></div>
          ) : pendingCharges.length === 0 ? (
            <p className="text-sm text-center text-slate-400 py-8">대기 중인 충전 신청이 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5 text-left font-semibold">신청번호</th>
                    <th className="px-4 py-2.5 text-left font-semibold">가맹점</th>
                    <th className="px-4 py-2.5 text-left font-semibold">신청자</th>
                    <th className="px-4 py-2.5 text-right font-semibold">금액</th>
                    <th className="px-4 py-2.5 text-right font-semibold">신청일</th>
                    <th className="px-4 py-2.5 text-right font-semibold">처리</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {pendingCharges.map((c) => (
                    <tr key={c.id} className="hover:bg-amber-50/30">
                      <td className="px-4 py-3 font-semibold text-slate-800">#{c.id}</td>
                      <td className="px-4 py-3 text-slate-700">{c.storeName || '-'}</td>
                      <td className="px-4 py-3 text-slate-600">{c.requestUserName || '-'}</td>
                      <td className="px-4 py-3 text-right font-bold text-slate-800 tabular-nums">
                        {Number(c.amount || 0).toLocaleString('ko-KR')}원
                      </td>
                      <td className="px-4 py-3 text-right text-xs text-slate-500">
                        {c.requestDate ? new Date(c.requestDate).toLocaleString('ko-KR') : '-'}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            disabled={processingChargeId === c.id}
                            onClick={() => handleApprove(c.id)}
                            className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50"
                          >
                            {processingChargeId === c.id ? '...' : '승인'}
                          </button>
                          <button
                            type="button"
                            disabled={processingChargeId === c.id}
                            onClick={() => handleReject(c.id)}
                            className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 disabled:opacity-50"
                          >
                            반려
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {mainTab === 'history' && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white p-6 rounded-2xl border border-indigo-100 shadow-sm flex items-center justify-between relative overflow-hidden">
              <div className="absolute top-0 left-0 w-1.5 h-full bg-indigo-500" />
              <div>
                <p className="text-xs font-semibold text-slate-500">총 예치금 잔액 (본사 보관금)</p>
                <p className="text-2xl font-extrabold text-slate-800 mt-1">{summary.totalBalance.toLocaleString()}원</p>
              </div>
            </div>
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs font-semibold text-slate-500">이번 달 총 충전액</p>
              <p className="text-2xl font-extrabold text-emerald-600 mt-1">+{summary.monthlyCharge.toLocaleString()}원</p>
            </div>
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
              <p className="text-xs font-semibold text-slate-500">이번 달 발주 사용액</p>
              <p className="text-2xl font-extrabold text-rose-600 mt-1">-{summary.monthlyUsage.toLocaleString()}원</p>
            </div>
          </div>

          <form onSubmit={handleSearch} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap gap-3 items-center">
            <select value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value })} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[140px]">
              <option value="">모든 거래</option>
              <option value="CHARGE">충전 내역만</option>
              <option value="PAYMENT">결제 내역만</option>
            </select>
            <div className="flex-1 flex min-w-[200px] relative">
              <input
                type="text"
                placeholder="가맹점 이름으로 검색"
                value={filters.keyword}
                onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
                className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
            </div>
            <button type="submit" className="px-5 py-2 bg-slate-800 text-white text-sm font-bold rounded-xl hover:bg-slate-900 transition-colors">조회</button>
          </form>

          {loading ? (
            <div className="flex justify-center py-20"><Spinner size="lg" /></div>
          ) : histories.length === 0 ? (
            <EmptyState message="조건에 맞는 예치금 내역이 없습니다." />
          ) : (
            <>
              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200">
                    <thead className="bg-slate-50">
                      <tr>
                        {['발생 일시', '가맹점명', '구분', '상세 내용', '발생 금액', '거래 후 잔액'].map((h) => (
                          <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {histories.map((history) => (
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
        </>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-xl overflow-hidden">
            <div className="px-6 py-5 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-800">가맹점 예치금 수동 지급</h3>
              <p className="text-xs text-slate-500 mt-1">신청 없이 본사가 직접 잔액에 반영합니다. (프로모션·보정 등)</p>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">대상 가맹점 선택</label>
                {isFetchingUsers ? (
                  <div className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-400 flex items-center gap-2">
                    <Spinner size="sm" /> 가맹점 목록을 불러오는 중...
                  </div>
                ) : (
                  <select
                    value={chargeData.targetUserId}
                    onChange={(e) => setChargeData({ ...chargeData, targetUserId: e.target.value })}
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
                  onChange={(e) => setChargeData({ ...chargeData, description: e.target.value })}
                  placeholder="예: 본사 프로모션 지원금"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-2">
              <button
                type="button"
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
                type="button"
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
