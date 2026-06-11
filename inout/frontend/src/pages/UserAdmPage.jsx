import { useState, useEffect, useCallback } from 'react';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';

import { getUserList, unlockUserAccount, updateUserByAdmin, sendPasswordResetEmail } from '../api/adminUserApi';

const PAGE_SIZE = 10;

function StatusBadge({ status }) {
  const config = {
    ACTIVE:   { label: '재직 중', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    LEAVE:    { label: '휴직',    cls: 'bg-amber-100 text-amber-700 border-amber-200' },
    RESIGNED: { label: '퇴사',    cls: 'bg-slate-100 text-slate-500 border-slate-200' },
  }[status] || { label: status, cls: 'bg-slate-100 text-slate-600' };

  return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${config.cls}`}>{config.label}</span>;
}

function AccountBadge({ isLocked }) {
  if (isLocked) return <span className="flex items-center gap-1 text-rose-600 text-[11px] font-bold bg-rose-50 px-2 py-1 rounded border border-rose-100"><svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" /></svg>잠김</span>;
  return <span className="text-blue-600 text-[11px] font-bold bg-blue-50 px-2 py-1 rounded border border-blue-100">정상</span>;
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

function UserManageModal({ user, onClose, onRefresh }) {

  const [formData, setFormData] = useState({
    status: user.status,
    storeId: user.storeId || '',
    isAdmin: user.isAdmin || false,
  });
  const [saving, setSaving] = useState(false);

  const handleUnlock = async () => {
    if (!window.confirm(`${user.name} 님의 계정 잠금을 해제하시겠습니까?`)) return;
    try {
       await unlockUserAccount(user.id); 
      Toast.success('계정 잠금이 해제되었습니다.');
      onRefresh();
      onClose();
    } catch (err) {}
  };

  const handleResetPassword = async () => {
    if (!window.confirm('해당 사용자의 이메일로 비밀번호 초기화 링크를 발송하시겠습니까?')) return;
    try {
    await sendPasswordResetEmail({ email: user.email, name: user.name, phone: user.phone });
      Toast.success('비밀번호 초기화 메일이 발송되었습니다.');
    } catch (err) {}
  };

  const handleSave = async () => {
    setSaving(true);
    try {
     await updateUserByAdmin(user.id, formData);
      Toast.success('회원 정보가 성공적으로 변경되었습니다.');
      onRefresh();
      onClose();
    } catch (err) {
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
        {/* 헤더 */}
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
          <h2 className="text-lg font-bold text-slate-800">회원 상세 관리</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700">✕</button>
        </div>

        <div className="p-6 space-y-6 overflow-y-auto">
          {/* 기본 정보 요약 */}
          <div className="flex items-center gap-4 bg-indigo-50/50 p-4 rounded-xl border border-indigo-100">
            <div className="w-12 h-12 bg-indigo-600 text-white rounded-full flex items-center justify-center text-xl font-bold shadow-sm shrink-0">
              {user.name.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-800">{user.name}</h3>
                {user.isAdmin && <span className="bg-indigo-600 text-white text-[10px] px-1.5 py-0.5 rounded font-bold">ADMIN</span>}
              </div>
              <p className="text-sm text-slate-500 truncate">{user.email}</p>
              <p className="text-xs text-slate-400 mt-0.5">{user.phone}</p>
            </div>
            {user.isLocked && (
              <button onClick={handleUnlock} className="px-3 py-1.5 bg-rose-600 text-white text-xs font-bold rounded-lg shadow-sm hover:bg-rose-700 shrink-0">
                잠금 해제
              </button>
            )}
          </div>

          {/* 정보 수정 폼 */}
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">소속 매장</label>
              <select 
                value={formData.storeId} 
                onChange={(e) => setFormData({...formData, storeId: e.target.value})}
                className="w-full px-3 py-2 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="0">본점 (소속 매장 없음)</option>
                <option value="1">강남 1호점</option>
                <option value="2">홍대 2호점</option>
                <option value="3">부산 3호점</option>
              </select>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">재직 상태</label>
                <select 
                  value={formData.status} 
                  onChange={(e) => setFormData({...formData, status: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="ACTIVE">재직 중</option>
                  <option value="LEAVE">휴직</option>
                  <option value="RESIGNED">퇴사</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">권한 승격</label>
                <div className="flex items-center h-9 mt-1">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" checked={formData.isAdmin} onChange={(e) => setFormData({...formData, isAdmin: e.target.checked})} className="sr-only peer" />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                    <span className="ml-3 text-sm font-medium text-slate-700">관리자(ADMIN) 권한</span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100">
             <button onClick={handleResetPassword} className="text-sm font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
               <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" /></svg>
               비밀번호 초기화 메일 발송
             </button>
             <p className="text-xs text-slate-400 mt-1 pl-5">사용자의 이메일로 비밀번호 재설정 링크를 전송합니다.</p>
          </div>
        </div>


        <div className="p-4 border-t border-slate-100 bg-slate-50 flex gap-2 justify-end">
          <button onClick={onClose} className="px-5 py-2 text-sm font-bold text-slate-600 bg-white border border-slate-300 rounded-xl hover:bg-slate-50">취소</button>
          <button onClick={handleSave} disabled={saving} className="px-5 py-2 text-sm font-bold text-white bg-slate-800 rounded-xl hover:bg-slate-900 disabled:opacity-50 flex items-center">
            {saving ? <Spinner size="sm" /> : '변경사항 저장'}
          </button>
        </div>
      </div>
    </div>
  );
}


export default function UserAdmPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedUser, setSelectedUser] = useState(null);
  const [summary, setSummary] = useState({ total: 0, active: 0, leave: 0, locked: 0 });
  const [filters, setFilters] = useState({ keyword: '', status: '', storeId: '' });
  const loadUsers = useCallback((pg) => {
    setLoading(true);
    
 
    getUserList({ page: pg, size: PAGE_SIZE, ...filters })
      .then(data => {
        setUsers(data.users.content); 
        setTotalPages(data.users.totalPages);
        setSummary(data.summary); 
      })
      .catch(() => Toast.error('회원 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false));

  }, [filters]);

  useEffect(() => { loadUsers(page); }, [page, loadUsers]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadUsers(0);
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-slate-800">회원 관리</h2>
        <p className="text-sm text-slate-500 mt-0.5">시스템에 등록된 전체 직원의 계정 상태와 권한을 관리합니다.</p>
      </div>

      {/* ── 1. 상단 요약 위젯 ── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">총 직원</p><p className="text-2xl font-extrabold text-slate-800 mt-1">{summary.total}명</p></div>
          <div className="w-10 h-10 rounded-full bg-slate-50 flex items-center justify-center text-slate-400"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" /></svg></div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">재직 중</p><p className="text-2xl font-extrabold text-emerald-600 mt-1">{summary.active}명</p></div>
          <div className="w-10 h-10 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg></div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div><p className="text-xs font-semibold text-slate-500">휴직</p><p className="text-2xl font-extrabold text-amber-500 mt-1">{summary.leave}명</p></div>
          <div className="w-10 h-10 rounded-full bg-amber-50 flex items-center justify-center text-amber-500"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M14.25 9v6m-4.5 0V9M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg></div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-rose-200 shadow-sm flex items-center justify-between bg-rose-50/30">
          <div><p className="text-xs font-semibold text-rose-600">잠긴 계정</p><p className="text-2xl font-extrabold text-rose-600 mt-1">{summary.locked}건</p></div>
          <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center text-rose-500"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" /></svg></div>
        </div>
      </div>

      {/* ── 2. 필터 및 검색 영역 ── */}
      <form onSubmit={handleSearch} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap gap-3 items-center">
        <select value={filters.storeId} onChange={(e) => setFilters({...filters, storeId: e.target.value})} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[140px]">
          <option value="">모든 매장</option>
          <option value="1">본점</option>
          <option value="2">강남 1호점</option> 
          <option value="3">홍대 2호점</option>
          
          <option value="0">소속 없음(미지정)</option>
        </select>
        <select value={filters.status} onChange={(e) => setFilters({...filters, status: e.target.value})} className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 focus:outline-none min-w-[120px]">
          <option value="">모든 상태</option>
          <option value="ACTIVE">재직 중</option>
          <option value="LEAVE">휴직</option>
          <option value="RESIGNED">퇴사</option>
        </select>
        <div className="flex-1 flex min-w-[200px] relative">
          <input 
            type="text" 
            placeholder="이름, 이메일, 연락처 검색" 
            value={filters.keyword} 
            onChange={(e) => setFilters({...filters, keyword: e.target.value})}
            className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        </div>
        <button type="submit" className="px-5 py-2 bg-slate-800 text-white text-sm font-bold rounded-xl hover:bg-slate-900 transition-colors">검색</button>
      </form>

      {/* ── 3. 테이블 영역 ── */}
      {loading ? ( <div className="flex justify-center py-20"><Spinner size="lg" /></div> ) 
      : users.length === 0 ? ( <EmptyState message="조건에 맞는 직원이 없습니다." /> ) 
      : (
        <>
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    {['회원 정보', '소속 매장', '상태', '계정 잠금', '가입일'].map(h => (
                      <th key={h} className="px-5 py-3 text-left text-[11px] font-semibold text-slate-500 uppercase">{h}</th>
                    ))}
                    <th className="px-5 py-3 text-center text-[11px] font-semibold text-slate-500 uppercase">관리</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {users.map(user => (
                    <tr key={user.id} className="hover:bg-slate-50 transition-colors">
                      {/* 이름(이메일) */}
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold ${user.isAdmin ? 'bg-indigo-600' : 'bg-slate-400'}`}>
                            {user.name.charAt(0)}
                          </div>
                          <div>
                            <p className="text-sm font-bold text-slate-800 flex items-center gap-1.5">
                              {user.name}
                              {user.isAdmin && <span className="bg-indigo-100 text-indigo-700 text-[9px] px-1 rounded font-bold uppercase tracking-wider">Admin</span>}
                            </p>
                            <p className="text-[11px] text-slate-400">{user.email}</p>
                          </div>
                        </div>
                      </td>
                      {/* 매장명 */}
                      <td className="px-5 py-4 text-sm font-medium text-slate-600">
                        {user.storeName || <span className="text-slate-400 text-xs">본사 소속</span>}
                      </td>
                      {/* 상태 */}
                      <td className="px-5 py-4"><StatusBadge status={user.status} /></td>
                      {/* 계정상태 */}
                      <td className="px-5 py-4"><AccountBadge isLocked={user.isLocked} /></td>
                      {/* 가입일 */}
                      <td className="px-5 py-4 text-xs text-slate-500">
                        {new Date(user.createdAt).toLocaleDateString('ko-KR')}
                      </td>
                      {/* 관리 버튼 */}
                      <td className="px-5 py-4 text-center">
                        <button 
                          onClick={() => setSelectedUser(user)}
                          className="px-3 py-1.5 border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-100 hover:border-slate-300 transition-all shadow-sm"
                        >
                          관리
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div className="flex justify-center"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
        </>
      )}

      {/* ── 4. 관리 모달 (Action Area) ── */}
      {selectedUser && (
        <UserManageModal 
          user={selectedUser} 
          onClose={() => setSelectedUser(null)} 
          onRefresh={() => loadUsers(page)} 
        />
      )}
    </div>
  );
}