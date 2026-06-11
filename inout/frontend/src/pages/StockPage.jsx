import { useState, useEffect, useCallback } from 'react';
import { getList, getHistory, getLowStockAlerts, adjustStock, registerStock } from '../api/stockApi';
import { getDashboardSummary } from '../api/dashboardApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

function StatusBadge({ item }) {
  const { currentStock, minStockLevel, deleted } = item;
  if (deleted) return (
    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-500">비활성</span>
  );
  if (currentStock === 0) return (
    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-700">
      <span className="w-1.5 h-1.5 rounded-full bg-rose-500 inline-block" />품절
    </span>
  );
  if (currentStock <= minStockLevel) return (
    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-700">
      <span className="w-1.5 h-1.5 rounded-full bg-amber-500 inline-block" />저재고
    </span>
  );
  return (
    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">
      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block" />정상
    </span>
  );
}

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

function RegisterModal({ onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    unitPrice: '',
    minStockLevel: '0',
    unitDescription: '',
    description: ''
  });
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        ...formData,
        categoryId: Number(formData.categoryId),
        unitPrice: Number(formData.unitPrice),
        minStockLevel: Number(formData.minStockLevel || 0),
      };
      await registerStock(payload);
      Toast.success('신규 상품이 등록되었습니다.');
      onSuccess();
      onClose();
    } catch {
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 className="text-base font-bold text-slate-800">신규 상품 등록</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 transition-colors p-1 rounded-lg hover:bg-slate-200">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4 overflow-y-auto max-h-[70vh]">
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">상품명 <span className="text-rose-500">*</span></label>
            <input type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="예: 최고급 콜롬비아 원두 1kg"
              className="w-full px-4 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all" />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">카테고리 <span className="text-rose-500">*</span></label>
              <select name="categoryId" value={formData.categoryId} onChange={handleChange} required
                className="w-full px-4 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all">
                <option value="" disabled>선택하세요</option>
                <option value="1">커피/음료 (1)</option>
                <option value="2">베이커리/디저트 (2)</option>
                <option value="3">포장재/소모품 (3)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">단가(원) <span className="text-rose-500">*</span></label>
              <input type="number" min="0" name="unitPrice" value={formData.unitPrice} onChange={handleChange} required placeholder="0"
                className="w-full px-4 py-2 text-sm text-right bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">안전 재고 (최소 기준치)</label>
              <input type="number" min="0" name="minStockLevel" value={formData.minStockLevel} onChange={handleChange} placeholder="0"
                className="w-full px-4 py-2 text-sm text-right bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">단위 설명</label>
              <input type="text" name="unitDescription" value={formData.unitDescription} onChange={handleChange} placeholder="예: 박스, 개, kg"
                className="w-full px-4 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">상세 설명</label>
            <textarea name="description" value={formData.description} onChange={handleChange} rows="3" placeholder="상품에 대한 상세한 설명을 입력하세요."
              className="w-full px-4 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all resize-none" />
          </div>

          <div className="flex gap-2 pt-4 border-t border-slate-100">
            <button type="button" onClick={onClose} className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors">취소</button>
            <button type="submit" disabled={submitting} className="flex-1 py-2.5 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {submitting ? '등록 중...' : '상품 등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


function HistoryModal({ item, onClose }) {
  const [loading, setLoading] = useState(true);
  const [history, setHistory] = useState([]);
  const [page, setPage]       = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const PAGE = 20;

  useEffect(() => {
    if (!item) return;
    setLoading(true);
    getHistory(item.itemId, page, PAGE)
      .then((data) => {
        const list = Array.isArray(data) ? data : (data.content || []);
        setHistory(list);
        setHasMore(list.length === PAGE);
      })
      .catch(() => Toast.error('이력을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [item, page]);

  const formatDate = (val) => {
    if (!val) return '-';
    const d = new Date(val);
    return isNaN(d) ? val : d.toLocaleString('ko-KR');
  };

  const typeStyle = (type) => type === '입고'
    ? 'bg-blue-50 text-blue-700 border border-blue-100'
    : 'bg-orange-50 text-orange-700 border border-orange-100';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col overflow-hidden">
        <div className="flex items-start justify-between px-6 py-4 border-b border-slate-200 shrink-0">
          <div>
            <h2 className="text-base font-semibold text-slate-800">재고 이력</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              <span className="font-medium text-slate-700">{item.name}</span>&nbsp;·&nbsp;{item.categoryName}
            </p>
          </div>
          <button onClick={onClose}
            className="text-slate-400 hover:text-slate-700 transition-colors p-1 rounded-lg hover:bg-slate-100 -mt-1 -mr-1">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center py-16"><Spinner size="lg" /></div>
          ) : history.length === 0 ? (
            <EmptyState message="등록된 이력이 없습니다." />
          ) : (
            <table className="min-w-full divide-y divide-slate-100">
              <thead className="bg-slate-50 sticky top-0">
                <tr>
                  {['구분', '날짜', '수량', '처리 후 재고', '담당자', '비고'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {history.map((row, idx) => (
                  <tr key={row.historyId || idx} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <span className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-semibold ${typeStyle(row.type)}`}>
                        {row.type}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600 whitespace-nowrap">{formatDate(row.date)}</td>
                    <td className="px-4 py-3 text-sm font-semibold text-slate-800">
                      {row.type === '입고' ? '+' : '-'}{(row.quantity ?? 0).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-700">{(row.resultStock ?? '-').toLocaleString()}</td>
                    <td className="px-4 py-3 text-xs text-slate-600">{row.workerName || '-'}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 max-w-[120px] truncate" title={row.remarks}>
                      {row.remarks || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {!loading && history.length > 0 && (
          <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between shrink-0">
            <span className="text-xs text-slate-400">최신순 {PAGE}건씩 조회</span>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 disabled:opacity-40 hover:bg-slate-50 transition-colors">
                이전
              </button>
              <span className="px-3 py-1.5 text-xs text-slate-500">{page + 1}페이지</span>
              <button disabled={!hasMore} onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 disabled:opacity-40 hover:bg-slate-50 transition-colors">
                다음
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function AdjustModal({ item, onClose, onSuccess }) {
  const [qty, setQty]       = useState(item.currentStock ?? 0);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const diff = qty - (item.currentStock ?? 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) { Toast.error('조정 사유를 입력해주세요.'); return; }
    setSubmitting(true);
    try {
      await adjustStock(item.itemId, qty, reason.trim());
      Toast.success(`재고 실사가 완료되었습니다. (${item.name})`);
      onSuccess();
      onClose();
    } catch {

    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* 헤더 */}
        <div className="flex items-start justify-between px-6 py-4 border-b border-slate-200">
          <div>
            <h2 className="text-base font-semibold text-slate-800">재고 실사</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              <span className="font-medium text-slate-700">{item.name}</span>
              &nbsp;·&nbsp;현재 재고&nbsp;
              <span className="font-semibold text-indigo-600">{(item.currentStock ?? 0).toLocaleString()}</span>
              {item.unitDescription && <span className="ml-0.5 text-slate-400">{item.unitDescription}</span>}
            </p>
          </div>
          <button onClick={onClose}
            className="text-slate-400 hover:text-slate-700 transition-colors p-1 rounded-lg hover:bg-slate-100 -mt-1 -mr-1">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
              실사 후 실제 수량
            </label>
            <input
              type="number"
              min="0"
              value={qty}
              onChange={(e) => setQty(Math.max(0, Number(e.target.value)))}
              required
              className="w-full px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all"
            />
            {/* 차이 미리보기 */}
            {diff !== 0 && (
              <p className={`mt-1.5 text-xs font-medium ${diff > 0 ? 'text-blue-600' : 'text-rose-600'}`}>
                {diff > 0
                  ? `▲ ${diff.toLocaleString()} 증가 (입고 이력으로 기록)`
                  : `▼ ${Math.abs(diff).toLocaleString()} 감소 (사용 이력으로 기록)`}
              </p>
            )}
            {diff === 0 && (
              <p className="mt-1.5 text-xs text-slate-400">현재 재고와 동일합니다. 변경 없음.</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
              조정 사유 <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="예: 월간 재고 실사, 파손·분실 반영"
              required
              maxLength={100}
              className="w-full px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all"
            />
          </div>

          <div className="flex gap-2 pt-1">
            <button type="button" onClick={onClose}
              className="flex-1 py-2.5 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors">
              닫기
            </button>
            <button type="submit" disabled={submitting}
              className="flex-1 py-2.5 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {submitting ? '처리 중...' : '실사 완료'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


function StockTable({ items, onHistoryClick, onAdjustClick }) {
  const formatPrice = (val) =>
    val != null ? `${Number(val).toLocaleString('ko-KR')}원` : '-';

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {[
                { label: '상품명',   cls: 'text-left'   },
                { label: '카테고리',  cls: 'text-left'   },
                { label: '단가',      cls: 'text-right'  },
                { label: '현재 재고', cls: 'text-right'  },
                { label: '안전 재고', cls: 'text-right'  },
                { label: '상태',      cls: 'text-center' },
                { label: '이력',      cls: 'text-center' },
                { label: '재고 실사', cls: 'text-center' },
              ].map(({ label, cls }) => (
                <th key={label} className={`px-5 py-3 ${cls} text-[11px] font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap`}>
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.map((item) => (
              <tr key={item.itemId} className={`hover:bg-slate-50 transition-colors ${item.deleted ? 'opacity-50' : ''}`}>
                <td className="px-5 py-3.5"><span className="text-sm font-medium text-slate-800">{item.name}</span></td>
                <td className="px-5 py-3.5 text-sm text-slate-500">{item.categoryName || '-'}</td>
                <td className="px-5 py-3.5 text-sm text-slate-700 text-right font-medium">{formatPrice(item.unitPrice)}</td>
                <td className="px-5 py-3.5 text-right">
                  <span className={`text-sm font-bold ${
                    item.currentStock === 0 ? 'text-rose-600' :
                    item.currentStock <= item.minStockLevel ? 'text-amber-600' :
                    'text-slate-800'
                  }`}>
                    {(item.currentStock ?? 0).toLocaleString()}
                  </span>
                  {item.unitDescription && (
                    <span className="ml-1 text-xs text-slate-400">{item.unitDescription}</span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-sm text-slate-500 text-right">{(item.minStockLevel ?? 0).toLocaleString()}</td>
                <td className="px-5 py-3.5 text-center"><StatusBadge item={item} /></td>
                <td className="px-5 py-3.5 text-center">
                  <button onClick={() => onHistoryClick(item)}
                    className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-50 border border-slate-200 text-slate-600 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200 transition-all">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    이력
                  </button>
                </td>
                <td className="px-5 py-3.5 text-center">
                  {!item.deleted && (
                    <button onClick={() => onAdjustClick(item)}
                      className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-50 border border-slate-200 text-slate-600 hover:bg-violet-50 hover:text-violet-700 hover:border-violet-200 transition-all">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      실사
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function LowStockBanner({ count }) {
  if (!count) return null;
  return (
    <div className="flex items-center gap-3 px-4 py-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-800">
      <svg className="w-5 h-5 text-amber-500 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth="1.75" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
      </svg>
      <span>재고 보충이 필요한 상품이 <strong>{count}건</strong> 있습니다.</span>
    </div>
  );
}


export default function StockPage() {
  const [items, setItems]               = useState([]);
  const [loading, setLoading]           = useState(true);
  const [page, setPage]                 = useState(0);
  const [totalPages, setTotalPages]     = useState(0);
  const [totalElements, setTotal]       = useState(0);
  const [searchName, setSearchName]     = useState('');
  const [inputName, setInputName]       = useState('');
  const [showDeleted, setShowDeleted]   = useState(false);
  const [lowCount, setLowCount]         = useState(0);
  
  const [selectedItem, setSelectedItem] = useState(null);
  const [adjustItem, setAdjustItem]     = useState(null);
  const [isRegisterOpen, setRegisterOpen] = useState(false);

  const [summaryData, setSummaryData]   = useState(null);

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
      .catch(() => { Toast.error('재고 목록을 불러오지 못했습니다.'); setItems([]); })
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
          <button onClick={() => setRegisterOpen(true)}
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

      <LowStockBanner count={lowCount} />

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
        <StockTable items={items} onHistoryClick={setSelectedItem} onAdjustClick={setAdjustItem} />
      )}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination page={page} totalPages={totalPages}
            onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
        </div>
      )}

      {/* 모달 영역 */}
      {isRegisterOpen && (
        <RegisterModal 
          onClose={() => setRegisterOpen(false)} 
          onSuccess={handleRefresh} 
        />
      )}
      {selectedItem && (
        <HistoryModal item={selectedItem} onClose={() => setSelectedItem(null)} />
      )}
      {adjustItem && (
        <AdjustModal
          item={adjustItem}
          onClose={() => setAdjustItem(null)}
          onSuccess={handleRefresh}
        />
      )}
    </div>
  );
}