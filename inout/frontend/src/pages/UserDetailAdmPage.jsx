import { useState, useEffect } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Toast } from '../utils/toast.js';
import Spinner from '../components/common/Spinner';
import { unlockUserAccount, updateUserByAdmin, sendPasswordResetEmail } from '../api/adminUserApi.js';

export default function UserDetailAdmPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userId } = useParams();

  // 💡 목록 페이지에서 넘겨준 user 데이터를 받아서 초기화
  const [user, setUser] = useState(location.state?.user || null);

  const [formData, setFormData] = useState({
    status: user?.status || 'ACTIVE',
    storeId: user?.storeId || '',
    isAdmin: user?.isAdmin || false,
  });
  const [saving, setSaving] = useState(false);
  const [imageError, setImageError] = useState(false); // 💡 이미지 에러 상태 추가

  // 새로고침 등으로 state가 날아갔을 경우의 예외 처리
  useEffect(() => {
    if (!user) {
      Toast.error('회원 정보를 불러올 수 없습니다. 목록에서 다시 진입해주세요.');
      navigate('/admin/users');
    }
  }, [user, navigate]);

  if (!user) return null;

  const handleUnlock = async () => {
    if (!window.confirm(`${user.name} 님의 계정 잠금을 해제하시겠습니까?`)) return;
    try {
      await unlockUserAccount(user.id); 
      Toast.success('계정 잠금이 해제되었습니다.');
      setUser({ ...user, isLocked: false }); // 화면 즉시 업데이트
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
      navigate('/admin/users'); // 저장 후 목록으로 복귀
    } catch (err) {
    } finally {
      setSaving(false);
    }
  };

  // 💡 프로필 이미지 결정 (없거나 에러 시 기본 프로필 이미지 사용)
  // public 폴더에 default-profile.png 가 있다고 가정합니다.
  const imgSrc = (imageError || !user.profileImageUrl) ? '/default-profile.png' : user.profileImageUrl;

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      
      {/* 상단 네비게이션 헤더 */}
      <div className="flex items-center gap-3 mb-2">
        <button onClick={() => navigate('/admin/users')} className="text-slate-400 hover:text-indigo-600 transition-colors p-2 bg-white rounded-full shadow-sm border border-slate-200">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-800">회원 상세 관리</h2>
          <p className="text-sm text-slate-500">직원의 상세 정보를 확인하고 권한 및 상태를 변경합니다.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* 좌측: 직원 프로필 사진 및 요약 정보 */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col items-center">
            
            {/* 프로필 이미지 */}
            <div className="w-48 aspect-square bg-slate-100 rounded-full mb-6 overflow-hidden border-4 border-white shadow-md relative">
              {/* 만약 기본 이미지가 없고 엑스박스가 뜨면 이름의 첫 글자를 보여주는 폴백 UI 설정 가능 */}
              <img 
                src={imgSrc} 
                alt={user.name} 
                className="w-full h-full object-cover"
                onError={() => setImageError(true)}
              />
              {/* 이미지가 깨졌을 때 이니셜 표시 (선택사항) */}
              {imageError && !user.profileImageUrl && (
                <div className="absolute inset-0 flex items-center justify-center bg-indigo-100 text-indigo-500 text-6xl font-bold">
                  {user.name.charAt(0)}
                </div>
              )}
            </div>
            
            {/* 뱃지 및 이름 */}
            <div className="flex items-center gap-2 mb-2">
              <h1 className="text-2xl font-bold text-slate-800 text-center">{user.name}</h1>
              {user.isAdmin && <span className="bg-indigo-600 text-white text-[10px] px-1.5 py-0.5 rounded-md font-bold tracking-wider uppercase mt-1">Admin</span>}
            </div>
            <p className="text-sm font-medium text-slate-500 mb-1">{user.email}</p>
            <p className="text-sm text-slate-400">{user.phone}</p>
            
            {/* 상태 요약 박스 */}
            <div className="w-full mt-6 space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-100">
              <div className="flex justify-between text-sm items-center">
                <span className="text-slate-500 font-medium">재직 상태</span>
                <span className={`font-bold ${
                  user.status === 'ACTIVE' ? 'text-emerald-600' : 
                  user.status === 'LEAVE' ? 'text-amber-500' : 'text-slate-500'
                }`}>
                  {user.status === 'ACTIVE' ? '재직 중' : user.status === 'LEAVE' ? '휴직' : '퇴사'}
                </span>
              </div>
              <div className="flex justify-between text-sm items-center">
                <span className="text-slate-500 font-medium">계정 잠금</span>
                {user.isLocked ? (
                  <span className="font-bold text-rose-600 flex items-center gap-1">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" /></svg>
                    잠김
                  </span>
                ) : (
                  <span className="font-bold text-blue-600">정상</span>
                )}
              </div>
            </div>

            {/* 잠금 해제 버튼 */}
            {user.isLocked && (
              <button onClick={handleUnlock} className="mt-4 w-full py-2.5 bg-rose-50 text-rose-600 border border-rose-200 text-sm font-bold rounded-xl shadow-sm hover:bg-rose-100 transition-colors">
                계정 잠금 해제
              </button>
            )}
          </div>
        </div>

        {/* 우측: 상세 관리 폼 */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden flex flex-col h-full">
            
            <div className="p-6 md:p-8 flex-1 space-y-8">
              <h3 className="text-lg font-bold text-slate-800 border-b border-slate-100 pb-3">정보 및 권한 수정</h3>
              
              <div className="space-y-6 max-w-xl">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">소속 매장</label>
                  <select 
                    value={formData.storeId} 
                    onChange={(e) => setFormData({...formData, storeId: e.target.value})}
                    className="w-full px-4 py-3 border border-slate-200 bg-slate-50 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                  >
                    <option value="0">본점 (소속 매장 없음)</option>
                    <option value="1">강남 1호점</option>
                    <option value="2">홍대 2호점</option>
                    <option value="3">부산 3호점</option>
                  </select>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-bold text-slate-700 mb-2">재직 상태</label>
                    <select 
                      value={formData.status} 
                      onChange={(e) => setFormData({...formData, status: e.target.value})}
                      className="w-full px-4 py-3 border border-slate-200 bg-slate-50 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                    >
                      <option value="ACTIVE">재직 중</option>
                      <option value="LEAVE">휴직</option>
                      <option value="RESIGNED">퇴사</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-bold text-slate-700 mb-2">권한 승격</label>
                    <div className="flex items-center h-[46px] px-2 bg-slate-50 rounded-xl border border-slate-200">
                      <label className="relative inline-flex items-center cursor-pointer w-full justify-between">
                        <span className="text-sm font-medium text-slate-700">관리자(ADMIN) 권한 부여</span>
                        <input type="checkbox" checked={formData.isAdmin} onChange={(e) => setFormData({...formData, isAdmin: e.target.checked})} className="sr-only peer" />
                        <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:right-[22px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="pt-6 mt-6 border-t border-slate-100">
                  <h4 className="text-sm font-bold text-slate-800 mb-3">보안 설정</h4>
                  <button onClick={handleResetPassword} className="px-5 py-2.5 bg-slate-100 text-sm font-bold text-indigo-600 border border-slate-200 rounded-xl hover:bg-indigo-50 hover:border-indigo-200 hover:text-indigo-700 flex items-center gap-2 transition-all">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" /></svg>
                    비밀번호 초기화 메일 발송
                  </button>
                  <p className="text-xs text-slate-400 mt-2">클릭 시 사용자의 이메일로 비밀번호 재설정 링크를 전송합니다.</p>
                </div>
              </div>
            </div>

            {/* 하단 버튼 영역 */}
            <div className="p-6 bg-slate-50 border-t border-slate-100 flex gap-3 justify-end shrink-0">
              <button onClick={() => navigate('/admin/users')} className="px-8 py-3 text-sm font-medium text-slate-600 bg-white border border-slate-300 rounded-xl hover:bg-slate-100 transition-colors shadow-sm">
                취소
              </button>
              <button onClick={handleSave} disabled={saving} className="px-8 py-3 text-sm font-bold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 disabled:opacity-50 transition-colors flex items-center shadow-sm">
                {saving ? <Spinner size="sm" className="mr-2" /> : null}
                변경사항 저장
              </button>
            </div>

          </div>
        </div>

      </div>
    </div>
  );
}