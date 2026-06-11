import { useState, useEffect, useCallback } from 'react';
import { getEmpStockList, useEmpStock } from '../api/stockEmpApi';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

export default function StockUseEmpPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  

  const [searchName, setSearchName] = useState('');
  const [inputName, setInputName] = useState('');

  const [useInputs, setUseInputs] = useState({});
  const [submittingId, setSubmittingId] = useState(null);

  const loadItems = useCallback((name) => {
    setLoading(true);
    getEmpStockList({ name: name || undefined, page: 0, size: 50 }) 
      .then((data) => {
        setItems(data?.content || []);
      })
      .catch(() => Toast.error('상품 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadItems(searchName); }, [searchName, loadItems]);


  const handleSearch = (e) => {
    e.preventDefault();
    setSearchName(inputName.trim());
  };

  const handleInputChange = (itemId, value) => {
    setUseInputs((prev) => ({ ...prev, [itemId]: value }));
  };

  const handleUseStock = async (item) => {
    const qty = Number(useInputs[item.itemId]);

    if (!qty || qty < 1) {
      return Toast.warning('사용할 수량을 1개 이상 입력해주세요.');
    }
    if (qty > item.currentStock) {
      return Toast.warning('현재 재고보다 많이 사용할 수 없습니다.');
    }

    if (!window.confirm(`[${item.name}] ${qty}개를 사용 처리하시겠습니까?`)) return;

    setSubmittingId(item.itemId);
    try {
      await useEmpStock({ 
        itemId: item.itemId, 
        quantity: qty, 
        memo: '매장 내 직접 사용' 
      });
      Toast.success('재고가 성공적으로 차감되었습니다.');

      setUseInputs((prev) => ({ ...prev, [item.itemId]: '' }));
      loadItems(searchName);
    } catch (err) {

    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <div className="space-y-5 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">재고 사용 처리</h2>
        <p className="text-sm text-slate-500 mt-0.5">매장에서 소진된 재고의 수량을 입력하여 차감 처리합니다.</p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm px-5 py-4 flex gap-3">
        {/* 기획서 D02: 카테고리 셀렉트박스 (향후 백엔드 지원 시 사용 가능하도록 UI만 배치) */}
        <select className="px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">전체 카테고리</option>
        </select>

        <form onSubmit={handleSearch} className="flex flex-1 gap-2">
          <input 
            type="text" 
            value={inputName} 
            onChange={(e) => setInputName(e.target.value)} 
            placeholder="물품명을 검색하세요..."
            className="flex-1 px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500" 
          />
          <button type="submit" className="px-6 py-2.5 text-sm font-semibold bg-slate-800 text-white rounded-xl hover:bg-slate-900 transition-colors">
            검색
          </button>
        </form>
      </div>

      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) 
      : items.length === 0 ? ( <EmptyState message="검색된 물품이 없습니다." /> ) 
      : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-5 py-3 text-center text-[11px] font-semibold text-slate-500 w-16">No</th>
                <th className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500">물품명 (D01)</th>
                <th className="px-5 py-3 text-center text-[11px] font-semibold text-slate-500 w-32">카테고리</th>
                <th className="px-5 py-3 text-right text-[11px] font-semibold text-slate-500 w-32">현재 재고</th>
                <th className="px-5 py-3 text-center text-[11px] font-semibold text-slate-500 w-64">사용 처리 (D04, D05)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((item, index) => {
                const inputValue = useInputs[item.itemId] || '';
                const isOutOfStock = item.currentStock === 0;
                const isOverStock = Number(inputValue) > item.currentStock;

                return (
                  <tr key={item.itemId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-5 py-4 text-center text-sm text-slate-400">{index + 1}</td>
                    <td className="px-5 py-4 text-sm font-bold text-slate-800">{item.name}</td>
                    <td className="px-5 py-4 text-center text-sm text-slate-500">{item.categoryName || '-'}</td>
                    <td className="px-5 py-4 text-right">
                      <span className={`text-sm font-bold ${isOutOfStock ? 'text-rose-500' : 'text-emerald-600'}`}>
                        {item.currentStock.toLocaleString()}개
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      {isOutOfStock ? (
                      
                        <div className="flex justify-end">
                          <span className="px-4 py-2 bg-rose-50 text-rose-600 text-sm font-bold rounded-lg border border-rose-100 w-full text-center">
                            입고 요청 필요
                          </span>
                        </div>
                      ) : (
                      
                        <div className="flex items-center justify-end gap-2">
                          <div className="flex items-center gap-1.5">
                            <input
                              type="number"
                              min="1"
                              max={item.currentStock}
                              value={inputValue}
                              onChange={(e) => handleInputChange(item.itemId, e.target.value)}
                              placeholder="0"
                              className={`w-16 px-2 py-1.5 text-right text-sm border rounded-lg focus:outline-none focus:ring-2 ${
                                isOverStock ? 'border-rose-400 focus:ring-rose-500' : 'border-slate-300 focus:ring-indigo-500'
                              }`}
                            />
                            <span className="text-sm text-slate-500 font-medium">개</span>
                          </div>
                          
                          <button 
                            onClick={() => handleUseStock(item)}
                            disabled={submittingId === item.itemId || isOverStock || !inputValue}
                            className="px-4 py-1.5 text-sm font-bold rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition-all min-w-[80px]"
                          >
                            {submittingId === item.itemId ? <Spinner size="sm" /> : '사용처리'}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}