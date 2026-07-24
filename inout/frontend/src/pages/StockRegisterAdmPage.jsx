import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerStock } from '../api/stockApi.js';
import { uploadImage } from '../api/imageApi.js';
import { Toast } from '../utils/toast.js';

export default function StockRegisterAdmPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    unitPrice: '',
    minStockLevel: '0',
    unitDescription: '',
    description: '',
    imageUrl: ''
  });
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      let uploadedUrl = formData.imageUrl || '';
      
      // 이미지가 첨부된 경우에만 업로드 API 호출
      if (imageFile) {
        const uploadResult = await uploadImage(imageFile);
        uploadedUrl = uploadResult.imageUrl || uploadResult; 
      }

      const payload = {
        ...formData,
        categoryId: Number(formData.categoryId),
        unitPrice: Number(formData.unitPrice),
        minStockLevel: Number(formData.minStockLevel || 0),
        imageUrl: uploadedUrl // 선택 사항이므로 비어 있을 수 있음
      };
      
      await registerStock(payload);
      Toast.success('신규 상품이 등록되었습니다.');
      navigate('/admin/stocks'); // 등록 성공 후 재고 목록으로 이동
    } catch (error) {
      // Toast 에러는 api 내에서 처리됨
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      
      {/* 상단 헤더 영역 */}
      <div className="flex items-center gap-3">
        <button 
          onClick={() => navigate('/admin/stocks')} 
          className="text-slate-400 hover:text-indigo-600 transition-colors p-2 bg-white rounded-full shadow-sm border border-slate-200"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-800">신규 상품 등록</h2>
          <p className="text-sm text-slate-500">새로운 재고 품목을 시스템에 추가합니다.</p>
        </div>
      </div>

      {/* 등록 폼 패널 */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <form onSubmit={handleSubmit} className="p-6 md:p-8">
          <div className="flex flex-col md:flex-row gap-8">
            
            {/* 좌측: 이미지 업로드 영역 */}
            <div className="w-full md:w-1/3 flex flex-col gap-2">
              <label className="block text-sm font-bold text-slate-700">대표 이미지 (선택)</label>
              <div 
                onClick={() => fileInputRef.current?.click()}
                className="w-full aspect-square border-2 border-dashed border-slate-300 rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:bg-slate-50 transition-colors overflow-hidden group mt-1"
              >
                {imagePreview ? (
                  <img src={imagePreview} alt="미리보기" className="w-full h-full object-cover group-hover:opacity-70 transition-opacity" />
                ) : (
                  <div className="text-slate-400 flex flex-col items-center gap-2">
                    <svg className="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v16m8-8H4" /></svg>
                    <span className="text-sm font-medium">이미지 첨부</span>
                  </div>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-2 text-center">정방형(1:1) 비율의 이미지를 권장합니다.</p>
              <input type="file" ref={fileInputRef} onChange={handleImageChange} accept="image/*" className="hidden" />
            </div>

            {/* 우측: 텍스트 폼 */}
            <div className="w-full md:w-2/3 space-y-6">
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-2">상품명 <span className="text-rose-500">*</span></label>
                <input type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="예: 최고급 콜롬비아 원두 1kg"
                  className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all" />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">카테고리 <span className="text-rose-500">*</span></label>
                  <select name="categoryId" value={formData.categoryId} onChange={handleChange} required
                    className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all">
                    <option value="" disabled>선택하세요</option>
                    <option value="1">커피/음료 (1)</option>
                    <option value="2">베이커리/디저트 (2)</option>
                    <option value="3">포장재/소모품 (3)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">단가(원) <span className="text-rose-500">*</span></label>
                  <input type="number" min="0" name="unitPrice" value={formData.unitPrice} onChange={handleChange} required placeholder="0"
                    className="w-full px-4 py-3 text-sm text-right bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all" />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">안전 재고 (최소 기준치)</label>
                  <input type="number" min="0" name="minStockLevel" value={formData.minStockLevel} onChange={handleChange} placeholder="0"
                    className="w-full px-4 py-3 text-sm text-right bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all" />
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">단위 설명</label>
                  <input type="text" name="unitDescription" value={formData.unitDescription} onChange={handleChange} placeholder="예: 박스, 개, kg"
                    className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all" />
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-slate-700 mb-2">상세 설명</label>
                <textarea name="description" value={formData.description} onChange={handleChange} rows="4" placeholder="상품에 대한 상세한 설명을 입력하세요."
                  className="w-full px-4 py-3 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 focus:bg-white transition-all resize-none" />
              </div>
            </div>
          </div>

          <div className="flex gap-3 pt-8 mt-6 border-t border-slate-100 justify-end">
            <button type="button" onClick={() => navigate('/admin/stocks')} className="px-8 py-3 text-sm font-medium bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors">취소</button>
            <button type="submit" disabled={submitting} className="px-8 py-3 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm">
              {submitting ? '등록 중...' : '상품 등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}