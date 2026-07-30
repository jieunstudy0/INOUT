import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { createInquiry } from '../api/inquiryApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

export default function InquiryCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleRemoveFile = () => {
    setFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      return Toast.warning('제목과 내용을 모두 입력해주세요.');
    }

    setSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('title', title.trim());
      formData.append('content', content.trim());
      if (file) {
        formData.append('file', file);
      } 
      await createInquiry(formData);
      Toast.success('문의가 등록되었습니다. AI가 카테고리·답변 초안을 준비합니다.');
      navigate('/emp/inquiries', { replace: true });
    } catch (err) {
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* 헤더 영역 */}
      <div>
        <button 
          onClick={() => navigate(-1)} 
          className="flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-slate-800 transition-colors mb-3"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
          뒤로 가기
        </button>
        <h2 className="text-2xl font-bold text-slate-800">1:1 문의 작성</h2>
        <p className="text-sm text-slate-500 mt-1">본사에 전달할 오류나 건의사항을 상세히 적어주세요. 필요한 경우 사진이나 문서를 첨부할 수 있습니다.</p>
      </div>

      <div className="rounded-2xl border border-violet-200 bg-violet-50/60 px-5 py-4 flex items-start gap-3">
        <span className="inline-flex w-8 h-8 shrink-0 items-center justify-center rounded-lg bg-violet-600 text-white text-xs font-bold">AI</span>
        <div>
          <p className="text-sm font-bold text-violet-800">AI 자동 카테고리 분류</p>
          <p className="text-xs text-violet-600/90 mt-1 leading-relaxed">
            문의 등록 후 Gemini가 내용을 분석해 추천 카테고리와 답변 초안을 생성합니다.
            본사 관리자가 상세 화면에서 AI 초안을 확인하고 적용할 수 있습니다.
          </p>
        </div>
      </div>

      {/* 작성 폼 영역 */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <form onSubmit={handleSubmit} className="p-8 space-y-6">
          
          {/* 제목 입력 */}
          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">제목 <span className="text-rose-500">*</span></label>
            <input 
              type="text" 
              value={title} 
              onChange={(e) => setTitle(e.target.value)} 
              placeholder="문의 제목을 입력하세요"
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all text-sm"
              required
            />
          </div>

          {/* 내용 입력 */}
          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">내용 <span className="text-rose-500">*</span></label>
            <textarea 
              value={content} 
              onChange={(e) => setContent(e.target.value)} 
              rows={12} 
              placeholder="문의하실 내용을 상세히 적어주세요."
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all text-sm resize-none"
              required
            />
          </div>

          {/* 첨부파일 입력 */}
          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">첨부 파일</label>
            <div className="flex items-center gap-4">
              <input 
                type="file" 
                ref={fileInputRef}
                onChange={handleFileChange} 
                className="hidden" 
                id="file-upload"
              />
              <label 
				  htmlFor="file-upload" 
				  className="cursor-pointer flex items-center gap-2 px-4 py-2.5 bg-slate-100 text-slate-600 font-medium rounded-xl hover:bg-slate-200 transition-colors text-sm border border-slate-200"
				>
				  <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
				    <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
				  </svg>
				  파일 선택
				</label>
              
              {/* 선택된 파일 표시 */}
              {file && (
                <div className="flex items-center gap-2 px-3 py-2 bg-indigo-50 border border-indigo-100 rounded-lg text-sm text-indigo-700">
                  <span className="truncate max-w-[200px]">{file.name}</span>
                  <button type="button" onClick={handleRemoveFile} className="text-indigo-400 hover:text-rose-500">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
                  </button>
                </div>
              )}
            </div>
            <p className="text-xs text-slate-400 mt-2">10MB 이하의 이미지(JPG, PNG) 또는 문서(PDF) 파일을 첨부할 수 있습니다.</p>
          </div>

          {/* 하단 버튼 영역 */}
          <div className="pt-6 border-t border-slate-100 flex justify-end gap-3">
            <button 
              type="button" 
              onClick={() => navigate(-1)} 
              disabled={submitting}
              className="px-6 py-3 text-sm font-medium rounded-xl bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors"
            >
              취소
            </button>
            <button 
              type="submit" 
              disabled={submitting}
              className="flex items-center gap-2 px-8 py-3 text-sm font-bold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60 transition-all shadow-md shadow-indigo-200"
            >
              {submitting ? <Spinner size="sm" className="text-white" /> : '문의 등록하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}