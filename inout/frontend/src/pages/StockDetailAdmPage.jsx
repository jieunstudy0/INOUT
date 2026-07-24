import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getStockDetail, adjustStock } from '../api/stockApi.js';
import { Toast } from '../utils/toast.js';
import Spinner from '../components/common/Spinner';

export default function StockDetailAdmPage() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [imageError, setImageError] = useState(false);

  // 💡 [추가] 페이지 내장형 재고 실사 폼을 위한 상태
  const [adjustQty, setAdjustQty] = useState(0);
  const [adjustReason, setAdjustReason] = useState('');
  const [isAdjusting, setIsAdjusting] = useState(false);

  const fetchDetail = useCallback(() => {
    getStockDetail(itemId)
      .then((data) => {
        setDetail(data);
        setImageError(false); 
        // 💡 데이터를 불러올 때마다 실사 입력칸을 현재 재고로 초기화
        setAdjustQty(data.currentStock ?? 0);
        setAdjustReason('');
      })
      .catch(() => {
        Toast.error('상품 상세 정보를 불러오지 못했습니다.');
        navigate('/admin/stocks'); 
      })
      .finally(() => setLoading(false));
  }, [itemId, navigate]);

  useEffect(() => {
    setLoading(true);
    fetchDetail();
  }, [fetchDetail]);

  // 💡 [추가] 재고 실사 폼 제출 핸들러
  const handleAdjustSubmit = async (e) => {
    e.preventDefault();
    if (!adjustReason.trim()) { Toast.error('조정 사유를 입력해주세요.'); return; }
    
    setIsAdjusting(true);
    try {
      await adjustStock(itemId, adjustQty, adjustReason.trim());
      Toast.success(`재고 실사가 반영되었습니다. (${detail.itemName})`);
      fetchDetail(); // 💡 성공 시 상세 데이터 즉시 새로고침 (이력 및 재고 반영)
    } catch {
      // 에러는 api 내에서 Toast로 처리됨
    } finally {
      setIsAdjusting(false);
    }
  };

  if (loading) return <div className="flex justify-center py-32"><Spinner size="lg" /></div>;
  if (!detail) return null;

  const imgSrc = (imageError || !detail.imageUrl) ? '/default-item-image.png' : detail.imageUrl;
  
  // 💡 재고 증감량 계산
  const diff = adjustQty - (detail.currentStock ?? 0);
  
  return (
    <div className="max-w-7xl mx-auto space-y-6">
      
      {/* 상단 네비게이션 헤더 */}
      <div className="flex items-center gap-3 mb-2">
        <button onClick={() => navigate('/admin/stocks')} className="text-slate-400 hover:text-indigo-600 transition-colors p-2 bg-white rounded-full shadow-sm border border-slate-200">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-800">상품 상세 정보</h2>
          <p className="text-sm text-slate-500">상품의 현재 상태와 누적 이력을 확인하고 재고를 조정합니다.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 좌측: 상품 이미지, 기본 정보 및 💡내장형 재고 실사 폼 */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col items-center">
            {/* 이미지 */}
            <div className="w-full aspect-square bg-slate-100 rounded-2xl mb-6 overflow-hidden border border-slate-100">
              <img 
                src={imgSrc} 
                alt={detail.itemName} 
                className="w-full h-full object-cover"
                onError={() => setImageError(true)}
              />
            </div>
            
            <span className="px-3 py-1 bg-indigo-50 text-indigo-600 text-xs font-bold rounded-lg mb-3">
              {detail.categoryName}
            </span>
            <h1 className="text-2xl font-bold text-slate-800 text-center">{detail.itemName}</h1>
            
            {/* 상태 요약 */}
            <div className="w-full mt-6 space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-100">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 font-medium">현재 상태</span>
                <span className={`font-bold ${detail.currentStock === 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
                  {detail.status}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 font-medium">현재 재고</span>
                <span className="font-bold text-slate-800">{detail.currentStock?.toLocaleString()}개</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 font-medium">안전 재고 기준</span>
                <span className="font-bold text-slate-800">{detail.minStockLevel?.toLocaleString()}개</span>
              </div>
            </div>

            {/* 💡 [수정] 페이지 내장형 재고 실사 입력 폼 */}
            <div className="w-full mt-6 border-t border-slate-200 pt-6">
              <div className="flex items-center gap-2 mb-4">
                <svg className="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <h3 className="text-base font-bold text-slate-800">재고 실사 입력</h3>
              </div>
              
              <form onSubmit={handleAdjustSubmit} className="space-y-4">
                <div className="bg-indigo-50/50 p-4 rounded-xl border border-indigo-100">
                  <label className="block text-xs font-semibold text-slate-600 mb-2">실사 후 실제 수량</label>
                  <input 
                    type="number" min="0" value={adjustQty} 
                    onChange={(e) => setAdjustQty(Math.max(0, Number(e.target.value)))} required
                    className="w-full px-4 py-2.5 text-sm font-bold text-slate-800 bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all"
                  />
                  <div className="mt-2 h-4">
                    {diff !== 0 ? (
                      <p className={`text-xs font-bold ${diff > 0 ? 'text-blue-600' : 'text-rose-600'}`}>
                        {diff > 0 ? `▲ ${diff.toLocaleString()}개 증가 (입고 처리)` : `▼ ${Math.abs(diff).toLocaleString()}개 감소 (사용 처리)`}
                      </p>
                    ) : (
                      <p className="text-xs text-slate-400 font-medium">현재 재고와 동일합니다.</p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5">조정 사유 <span className="text-rose-500">*</span></label>
                  <input 
                    type="text" value={adjustReason} 
                    onChange={(e) => setAdjustReason(e.target.value)} 
                    placeholder="예: 월간 재고 실사, 파손 반영 등" required maxLength={100}
                    className="w-full px-4 py-2.5 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400 transition-all"
                  />
                </div>

                <button 
                  type="submit" 
                  disabled={isAdjusting || diff === 0} 
                  className="w-full py-3 mt-2 text-sm font-bold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
                >
                  {isAdjusting ? '반영 중...' : diff === 0 ? '수량 변경 없음' : '실사 반영하기'}
                </button>
              </form>
            </div>
          </div>
        </div>

        {/* 우측: 누적 통계 및 재고 입출고 이력 */}
        <div className="lg:col-span-2 space-y-6">
          <div className="grid grid-cols-2 gap-4">
             <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-slate-500 mb-1">누적 입고량</p>
                  <p className="text-2xl font-bold text-blue-600">{detail.totalReceived?.toLocaleString()}</p>
                </div>
                <div className="w-12 h-12 bg-blue-50 rounded-full flex items-center justify-center text-blue-500">📦</div>
             </div>
             <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-slate-500 mb-1">누적 사용량</p>
                  <p className="text-2xl font-bold text-orange-600">{detail.totalUsed?.toLocaleString()}</p>
                </div>
                <div className="w-12 h-12 bg-orange-50 rounded-full flex items-center justify-center text-orange-500">🛒</div>
             </div>
          </div>

          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6">
            <h3 className="text-lg font-bold text-slate-800 mb-4">재고 입출고 이력</h3>
            <div className="overflow-x-auto max-h-[500px]">
               {detail.history?.length === 0 ? (
                 <div className="py-10 text-center text-slate-500">이력이 없습니다.</div>
               ) : (
                 <table className="min-w-full divide-y divide-slate-100">
                    <thead className="bg-slate-50 sticky top-0">
                      <tr>
                        {['구분', '날짜', '수량', '처리 후 재고', '담당자'].map(h => <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold text-slate-500">{h}</th>)}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {detail.history.map((row, idx) => (
                        <tr key={idx} className="hover:bg-slate-50">
                          <td className="px-4 py-3 text-sm font-semibold">{row.type}</td>
                          <td className="px-4 py-3 text-xs text-slate-500">{new Date(row.date).toLocaleString()}</td>
                          <td className={`px-4 py-3 text-sm font-bold ${row.type === '입고' ? 'text-blue-600' : 'text-rose-600'}`}>
                            {row.type === '입고' ? '+' : '-'}{Math.abs(row.quantity)}
                          </td>
                          <td className="px-4 py-3 text-sm font-semibold text-slate-700">{row.resultStock}</td>
                          <td className="px-4 py-3 text-xs text-slate-600">{row.workerName}</td>
                        </tr>
                      ))}
                    </tbody>
                 </table>
               )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}