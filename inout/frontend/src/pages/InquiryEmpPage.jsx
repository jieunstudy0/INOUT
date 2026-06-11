import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getInquiryList } from '../api/inquiryApi'; 
import Spinner from '../components/common/Spinner';

const PAGE_SIZE = 10;

function InquiryStatusBadge({ isRead }) {
  if (isRead) return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700">답변 완료</span>;
  return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600">답변 대기</span>;
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


export default function InquiryEmpPage() {
  const navigate = useNavigate();
  const [inquiries, setInquiries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadInquiries = useCallback((pg) => {
    setLoading(true);
    getInquiryList(pg, PAGE_SIZE)
      .then((data) => { 
        setInquiries(data?.content || []); 
        setTotalPages(data?.totalPages || 0); 
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadInquiries(page); }, [page, loadInquiries]);

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-bold text-slate-800">1:1 문의 (본사 소통)</h2>
          <p className="text-sm text-slate-500 mt-0.5">본사에 시스템 오류나 건의사항을 문의할 수 있습니다.</p>
        </div>
        <button 
          onClick={() => navigate('/emp/inquiries/new')} 
          className="px-5 py-2.5 bg-indigo-600 text-white text-sm font-bold rounded-xl hover:bg-indigo-700 shadow-sm transition-all active:scale-[0.98]"
        >
          + 문의 작성하기
        </button>
      </div>

      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) : (
        <>
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>{['번호', '상태', '제목', '등록일'].map(h => <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">{h}</th>)}</tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {inquiries.map((item, index) => {
                  const rowKey = item.id || item.inquiryId || index;
                  return (
                    <tr 
                      key={rowKey} 
                      onClick={() => navigate(`/emp/inquiries/${rowKey}`)} 
                      className="hover:bg-slate-50 cursor-pointer transition-colors"
                    >
                      <td className="px-5 py-4 text-sm text-slate-400">{item.id || item.inquiryId}</td>
                      <td className="px-5 py-4"><InquiryStatusBadge isRead={item.isRead} /></td>
                      <td className="px-5 py-4 text-sm font-semibold text-slate-800">{item.title}</td>
                      <td className="px-5 py-4 text-sm text-slate-500">{new Date(item.createdAt).toLocaleDateString('ko-KR')}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="flex justify-center"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
        </>
      )}
    </div>
  );
}