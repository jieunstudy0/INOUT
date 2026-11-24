import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getList, getLowStockAlerts } from '../api/stockApi';
import ProductCard from '../components/common/ProductCard';
import { stockBadge, stockTone } from '../utils/stockStatus';
import { getDashboardSummary } from '../api/dashboardApi';
import { triggerAiAutoOrderAnalysis } from '../api/aiApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 12;

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;
  const start = Math.max(0, page - 2);
  const end   = Math.min(totalPages, start + 5);
  const pages = [];
  for (let i = start; i < end; i++) pages.push(i);
  const btnBase = 'w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors';

  return (
    <div className="flex items-center gap-1">
      <button disabled={page === 0} onClick={() => onPageChange(page - 1)}
        className={`${btnBase} ${page === 0 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
      </button>
      {pages.map((p) => (
        <button key={p} onClick={() => onPageChange(p)}
          className={`${btnBase} ${p === page ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}>
          {p + 1}
        </button>
      ))}
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}
        className={`${btnBase} ${page >= totalPages - 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-500 hover:bg-slate-100'}`}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
        </svg>
      </button>
    </div>
  );
}

function AiDraftResultModal({ savedCount, onNavigate, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
        <div className="px-6 py-5 bg-teal-600">
          <div className="flex items-center gap-3">
            <span className="text-white text-2xl">✦</span>
            <div>
              <h3 className="text-base font-bold text-white">AI 발주 초안 생성 완료</h3>
              <p className="text-sm text-white/80 mt-0.5">
                {savedCount > 0
                  ? `안전재고 부족 품목에 대한 발주 초안 ${savedCount}건이 생성되었습니다.`
                  : '현재 재고 보충이 필요한 품목이 없습니다.'}
              </p>
            </div>
          </div>
        </div>
        <div className="px-6 py-5 space-y-3">
          {savedCount > 0 && (
            <button
              onClick={onNavigate}
              className="w-full py-2.5 text-sm font-semibold rounded-xl bg-teal-600 text-white hover:bg-teal-700 transition"
            >
              발주 관리 → AI 발주 제안 탭으로 이동
            </button>
          )}
          <button
            onClick={onClose}
            className="w-full py-2.5 text-sm font-medium rounded-xl bg-slate-100 text-slate-700 hover:bg-slate-200 transition"
          >
            현재 페이지 유지
          </button>
        </div>
      </div>
    </div>
  );
}

function LowStockBanner({ count, onAiClick, aiRunning }) {
  if (!count) return null;
  return (
    <div className="flex items-center justify-between gap-3 px-4 py-3 bg-amber-50 border border-amber-200 rounded-xl">
      <div className="flex items-center gap-2.5 text-sm text-amber-800">
        <svg className="w-5 h-5 text-amber-500 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
        </svg>
        <span>재고 보충이 필요한 상품이 <strong>{count}건</strong> 있습니다.</span>
      </div>
      <button
        onClick={onAiClick}
        disabled={aiRunning}
        className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-60 transition-all shrink-0"
      >
        {aiRunning
          ? <><Spinner size="sm" className="text-white" /> 분석 중…</>
          : <>✦ AI 발주 추천 분석</>}
      </button>
    </div>
  );
}


export default function StockPage() {
  const navigate = useNavigate();

  const [items, setItems]               = useState([]);
  const [loading, setLoading]           = useState(true);
  const [page, setPage]                 = useState(0);
  const [totalPages, setTotalPages]     = useState(0);
  const [totalElements, setTotal]       = useState(0);
  const [searchName, setSearchName]     = useState('');
  const [inputName, setInputName]       = useState('');
  const [showDeleted, setShowDeleted]   = useState(false);
  const [lowCount, setLowCount]         = useState(0);


  const [summaryData, setSummaryData]   = useState(null);

  // AI 발주 초안 생성
  const [aiRunning, setAiRunning]           = useState(false);
  const [aiDraftResult, setAiDraftResult]   = useState(null); // { savedCount }|null

  const loadItems = useCallback((pg, name, deleted) => {
    setLoading(true);
    getList({ name: name || undefined, deleted, page: pg, size: PAGE_SIZE })
      .then((data) => {
        if (data?.content !== undefined) {
          setItems(data.content || []);
          setTotalPages(data.totalPages || 0);
          setTotal(data.totalElements || 0);
        } else {
          const list = Array.isArray(data) ? data : [];
          setItems(list);
          setTotalPages(1);
          setTotal(list.length);
        }
      })
      .catch(() => { setItems([]); })
      .finally(() => setLoading(false));
  }, []);

  const loadAlerts = useCallback(() => {
    getLowStockAlerts()
      .then((data) => setLowCount(Array.isArray(data) ? data.length : 0))
      .catch(() => {});
  }, []);

  const loadSummary = useCallback(() => {
    getDashboardSummary()
      .then((data) => setSummaryData(data))
      .catch(() => {});
  }, []);

  useEffect(() => { loadItems(page, searchName, showDeleted); }, [page, searchName, showDeleted, loadItems]);
  useEffect(() => { loadAlerts(); }, [loadAlerts]);
  useEffect(() => { loadSummary(); }, [loadSummary]);

  const handleSearch = (e) => { e.preventDefault(); setPage(0); setSearchName(inputName.trim()); };
  const handleReset  = () => { setInputName(''); setSearchName(''); setShowDeleted(false); setPage(0); };
  
  const handleRefresh = () => {
    loadItems(page, searchName, showDeleted);
    loadAlerts();
    loadSummary();
    Toast.info('목록을 새로고침했습니다.');
  };

  const handleAiAnalyze = async () => {
    setAiRunning(true);
    try {
      const data = await triggerAiAutoOrderAnalysis();
      setAiDraftResult({ savedCount: data?.savedCount ?? 0 });
    } catch {
      /* toasted by interceptor */
    } finally {
      setAiRunning(false);
    }
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">재고 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            상품 재고 현황 조회 및 이력 확인
            {totalElements > 0 && (
              <span className="ml-2 text-indigo-600 font-semibold">총 {totalElements.toLocaleString()}건</span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2">         
          <button onClick={() => navigate('/admin/stock/new')}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 transition-all shadow-sm">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            신규 상품 등록
          </button>
          <button onClick={handleRefresh}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
            새로고침
          </button>
        </div>
      </div>

      {summaryData && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-slate-500 mb-1">총 활성 상품</p>
            <p className="text-2xl font-bold text-slate-800">{summaryData.totalActiveStockCount?.toLocaleString()}건</p>
          </div>
          <div className="bg-emerald-50 p-5 rounded-2xl border border-emerald-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-emerald-600 mb-1">정상 재고</p>
            <p className="text-2xl font-bold text-emerald-700">{summaryData.normalStockCount?.toLocaleString()}건</p>
          </div>
          <div className="bg-amber-50 p-5 rounded-2xl border border-amber-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-amber-600 mb-1">저재고 경고</p>
            <p className="text-2xl font-bold text-amber-700">{(summaryData.lowStockCount - summaryData.outOfStockCount)?.toLocaleString()}건</p>
          </div>
          <div className="bg-rose-50 p-5 rounded-2xl border border-rose-100 shadow-sm flex flex-col justify-center">
            <p className="text-xs font-semibold text-rose-600 mb-1">품절</p>
            <p className="text-2xl font-bold text-rose-700">{summaryData.outOfStockCount?.toLocaleString()}건</p>
          </div>
        </div>
      )}

      <LowStockBanner count={lowCount} onAiClick={handleAiAnalyze} aiRunning={aiRunning} />

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm px-5 py-4">
        <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-3">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">상품명 검색</label>
            <div className="relative">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400"
                fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
              <input type="text" value={inputName} onChange={(e) => setInputName(e.target.value)}
                placeholder="상품명을 입력하세요"
                className="w-full pl-9 pr-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer select-none pb-2.5">
            <input type="checkbox" checked={showDeleted}
              onChange={(e) => { setShowDeleted(e.target.checked); setPage(0); }}
              className="w-4 h-4 rounded text-indigo-600 border-slate-300 focus:ring-indigo-500" />
            비활성 상품 포함
          </label>
          <div className="flex gap-2 pb-0">
            <button type="submit" className="px-5 py-2.5 text-sm font-semibold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 active:scale-[0.98] transition-all shadow-sm">검색</button>
            <button type="button" onClick={handleReset} className="px-4 py-2.5 text-sm font-medium bg-white border border-slate-200 text-slate-600 rounded-xl hover:bg-slate-50 transition-all">초기화</button>
          </div>
        </form>
      </div>

      {loading && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm flex items-center justify-center py-20 gap-3 text-slate-400">
          <Spinner size="lg" /><span className="text-sm">재고 데이터를 불러오는 중...</span>
        </div>
      )}
      {!loading && items.length === 0 && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm">
          <EmptyState message={searchName ? `'${searchName}'에 해당하는 상품이 없습니다.` : '등록된 상품이 없습니다.'} />
        </div>
      )}
      {!loading && items.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-x-6 gap-y-9">
          {items.map((item) => (
            <ProductCard key={item.itemId}
              imageUrl={item.imageUrl}
              name={item.name}
              dimmed={item.deleted}
              badges={item.deleted ? [{ label: '비활성', tone: 'gray' }] : [stockBadge(item.currentStock ?? 0, item.minStockLevel ?? 0)]}
              stock={{ value: item.currentStock ?? 0, unit: item.unitDescription, tone: stockTone(item.currentStock ?? 0, item.minStockLevel ?? 0) }}
              sub={`${item.categoryName || '미지정'} · ${(item.unitPrice ?? 0).toLocaleString('ko-KR')}원 · 안전재고 ${item.minStockLevel ?? 0}`}
              description={item.description}
              onClick={() => navigate(`/admin/stock/${item.itemId}`)} />
          ))}
        </div>
      )}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination page={page} totalPages={totalPages}
            onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
        </div>
      )}

      {/* AI 발주 초안 결과 모달 */}
      {aiDraftResult && (
        <AiDraftResultModal
          savedCount={aiDraftResult.savedCount}
          onNavigate={() => {
            setAiDraftResult(null);
            navigate('/admin/orders', { state: { activeTab: 'AI_PROPOSED' } });
          }}
          onClose={() => setAiDraftResult(null)}
        />
      )}

    </div>
  );
}