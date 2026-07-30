import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getInquiryList } from '../api/inquiryApi';
import { triggerAiCsClassification } from '../api/aiApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

const PAGE_SIZE = 10;

function InquiryStatusBadge({ isRead }) {
  if (isRead) return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">확인 완료</span>;
  return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-600">답변 대기</span>;
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

export default function InquiryAdmPage() {
  const navigate = useNavigate();

  const [inquiries, setInquiries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [aiRunning, setAiRunning] = useState(false);

  const loadInquiries = useCallback((pg) => {
    setLoading(true);
    getInquiryList(pg, PAGE_SIZE)
      .then((data) => {
        setInquiries(data?.content || []);
        setTotalPages(data?.totalPages || 0);
      })
      .catch(() => Toast.error('문의 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadInquiries(page); }, [page, loadInquiries]);

  const handleAiCsDraft = async () => {
    setAiRunning(true);
    try {
      const data = await triggerAiCsClassification();
      Toast.success(data?.message || 'AI 문의 초안 생성이 완료되었습니다.');
      loadInquiries(page);
    } catch {
      /* interceptor */
    } finally {
      setAiRunning(false);
    }
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">가맹점 문의 관리</h2>
          <p className="text-sm text-slate-500 mt-0.5">전체 가맹점의 문의사항을 확인하고 AI 초안을 참고해 답변합니다.</p>
        </div>
        <button
          type="button"
          onClick={handleAiCsDraft}
          disabled={aiRunning}
          className="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl bg-violet-600 text-white hover:bg-violet-700 disabled:opacity-60 shadow-sm transition-all"
        >
          {aiRunning ? (
            <><Spinner size="sm" className="text-white" /> 초안 생성 중…</>
          ) : (
            <>AI 문의 초안 생성</>
          )}
        </button>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden relative">
        {loading && (
          <div className="absolute inset-0 bg-white/60 z-10 flex items-center justify-center">
            <Spinner size="lg" />
          </div>
        )}
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>{['번호', '상태', '제목', 'AI 분류', '작성자(가맹점)', '등록일'].map(h => <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">{h}</th>)}</tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {inquiries.length === 0 && !loading && (
              <tr><td colSpan="6" className="py-10"><EmptyState message="등록된 문의 내역이 없습니다." /></td></tr>
            )}
            {inquiries.map((item, index) => {
              const validId = item.id || item.inquiryId;
              const rowKey = validId || `inquiry-${index}`;

              return (
                <tr
                  key={rowKey}
                  onClick={() => navigate(`/admin/inquiries/${validId}`)}
                  className="hover:bg-slate-50 cursor-pointer transition-colors"
                >
                  <td className="px-5 py-4 text-sm text-slate-400">{validId}</td>
                  <td className="px-5 py-4"><InquiryStatusBadge isRead={item.isRead} /></td>
                  <td className={`px-5 py-4 text-sm ${!item.isRead ? 'font-bold text-slate-900' : 'font-medium text-slate-700'}`}>{item.title}</td>
                  <td className="px-5 py-4">
                    {item.aiCategory || item.hasAiDraft ? (
                      <div className="flex flex-col gap-1">
                        {item.aiCategory && (
                          <span className="inline-flex w-fit px-2 py-0.5 rounded-full text-[10px] font-bold bg-violet-100 text-violet-700">
                            {item.aiCategory}
                          </span>
                        )}
                        {item.hasAiDraft && (
                          <span className="inline-flex w-fit px-2 py-0.5 rounded-full text-[10px] font-bold bg-teal-100 text-teal-700">
                            AI 초안
                          </span>
                        )}
                      </div>
                    ) : (
                      <span className="text-xs text-slate-300">—</span>
                    )}
                  </td>
                  <td className="px-5 py-4 text-sm text-slate-600">{item.authorName}</td>
                  <td className="px-5 py-4 text-sm text-slate-500">{new Date(item.createdAt).toLocaleDateString('ko-KR')}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="flex justify-center"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
    </div>
  );
}
