import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitLeave } from '../api/vacationEmpApi.js';
import { Toast } from '../utils/toast.js';
import { useAppBasePath } from '../utils/appPaths';

const LEAVE_TYPE_OPTIONS = [
  { value: 'ANNUAL',   label: '연차' },
  { value: 'HALF_DAY', label: '반차' },
  { value: 'SICK',     label: '병가' },
];

export default function VacationRegisterEmpPage() {
  const navigate = useNavigate();
  const base = useAppBasePath();
  const leaveRoot = base === '/owner' ? '/owner/vacation/my' : '/emp/vacation';

  const [formData, setFormData] = useState({
    startDate: '',
    endDate: '',
    type: 'ANNUAL',
    reason: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.startDate || !formData.endDate) {
      window.alert('시작일과 종료일을 모두 선택해주세요.');
      return;
    }

    // 1차 방어: 백엔드 호출 전, 브라우저 단에서 기간 유효성을 먼저 확인한다.
    if (new Date(formData.startDate) > new Date(formData.endDate)) {
      window.alert('시작일이 종료일보다 늦을 수 없습니다. 날짜를 다시 확인해주세요.');
      return;
    }

    setSubmitting(true);
    try {
      await submitLeave(formData);
      Toast.success('연차 신청이 완료되었습니다.');
      navigate(leaveRoot);
    } catch (error) {
      // Toast 에러는 apiClient 인터셉터에서 처리됨
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* 상단 헤더 영역 */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(leaveRoot)}
          className="text-slate-400 hover:text-indigo-600 transition-colors p-2 bg-white rounded-full shadow-sm border border-slate-200"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-800">연차 신청</h2>
          <p className="text-sm text-slate-500">사용할 연차(휴가)의 기간과 사유를 입력해주세요.</p>
        </div>
      </div>

      {/* 신청 폼 패널 */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <form onSubmit={handleSubmit} className="p-6 md:p-8 space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-bold text-slate-700 mb-2">시작일 <span className="text-rose-500">*</span></label>
              <input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleChange}
                required
                className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all"
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-slate-700 mb-2">종료일 <span className="text-rose-500">*</span></label>
              <input
                type="date"
                name="endDate"
                value={formData.endDate}
                min={formData.startDate || undefined}
                onChange={handleChange}
                required
                className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">연차 종류 <span className="text-rose-500">*</span></label>
            <select
              name="type"
              value={formData.type}
              onChange={handleChange}
              required
              className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all"
            >
              {LEAVE_TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">사유</label>
            <textarea
              name="reason"
              value={formData.reason}
              onChange={handleChange}
              rows="4"
              maxLength={500}
              placeholder="연차 신청 사유를 입력하세요. (선택)"
              className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all resize-none"
            />
          </div>

          <div className="flex gap-3 pt-4 border-t border-slate-100 justify-end">
            <button type="button" onClick={() => navigate(leaveRoot)} className="px-8 py-3 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors">
              취소
            </button>
            <button type="submit" disabled={submitting} className="px-8 py-3 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm">
              {submitting ? '신청 중...' : '연차 신청'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
