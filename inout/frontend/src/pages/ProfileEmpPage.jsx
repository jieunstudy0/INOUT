import { useState, useEffect } from 'react';
import client, { unwrap } from '../api/apiClient';
import { Toast } from '../utils/toast';
import Spinner from '../components/common/Spinner';

export default function ProfileEmpPage() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [pwModalOpen, setPwModalOpen] = useState(false); // 비밀번호 모달 상태

  const [profile, setProfile] = useState({
    loginId: '', storeName: '', name: '', birthday: '', phone: '', status: '',
  });

  const [formData, setFormData] = useState({
    storeName: '', name: '', phone: '',
  });

  const [pwData, setPwData] = useState({ password: '', newPassword: '', confirmPassword: '' });

  useEffect(() => { loadProfile(); }, []);

  const loadProfile = async () => {
    setLoading(true);
    try {
      const data = await unwrap(client.get('/user/profile'));
      setProfile({
        loginId: data.loginId || data.email || '', 
        storeName: data.storeName || '',
        name: data.name || '',
        birthday: data.birthday || '',
        phone: data.phone || '',
        status: data.status || 'ACTIVE',
      });
      setFormData({
        storeName: data.storeName || '',
        name: data.name || '',
        phone: data.phone || '',
      });
    } catch (err) {
      Toast.error('내 정보를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case 'ACTIVE': return <span className="px-3 py-1 bg-emerald-100 text-emerald-700 rounded-lg text-xs font-bold">재직 중</span>;
      case 'ON_LEAVE':
      case 'LEAVE':
        return <span className="px-3 py-1 bg-amber-100 text-amber-700 rounded-lg text-xs font-bold">휴직 상태</span>;
      case 'RESIGNED': return <span className="px-3 py-1 bg-rose-100 text-rose-700 rounded-lg text-xs font-bold">퇴사</span>;
      default: return <span className="px-3 py-1 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold">{status}</span>;
    }
  };


  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.phone.trim()) return Toast.warning('이름과 핸드폰 번호는 필수입니다.');

    setSubmitting(true);
    try {
      await unwrap(client.put('/user/profile', formData));
      Toast.success('정보가 성공적으로 수정되었습니다.');
      setIsEditing(false);
      loadProfile(); 
    } catch (err) {} finally { setSubmitting(false); }
  };


  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (!pwData.password || !pwData.newPassword || !pwData.confirmPassword) {
      return Toast.warning('모든 항목을 입력해주세요.');
    }
    if (pwData.newPassword !== pwData.confirmPassword) {
      return Toast.error('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
    }

    setSubmitting(true);
    try {
      await unwrap(client.patch('/user/profile/password', { password: pwData.password, newPassword: pwData.newPassword }));
      Toast.success('비밀번호가 성공적으로 변경되었습니다.');
      setPwModalOpen(false);
      setPwData({ password: '', newPassword: '', confirmPassword: '' });
    } catch (err) {} finally { setSubmitting(false); }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex justify-between items-end">
        <div>
          <h2 className="text-xl font-bold text-slate-800">내 정보</h2>
          <p className="text-sm text-slate-500 mt-0.5">내 프로필 정보를 확인하고 수정할 수 있습니다.</p>
        </div>
        {!isEditing && (
          <button onClick={() => setPwModalOpen(true)} className="px-4 py-2 text-sm font-semibold text-slate-600 bg-white border border-slate-300 rounded-lg hover:bg-slate-50">
            비밀번호 변경
          </button>
        )}
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden p-8">
        {!isEditing ? (
        
          <div className="space-y-6">
            <div className="grid grid-cols-3 gap-y-6 text-sm">
              <div className="col-span-1 text-slate-500 font-semibold">이메일</div>
              <div className="col-span-2 text-slate-800 font-medium">{profile.loginId}</div>

              <div className="col-span-1 text-slate-500 font-semibold">이름</div>
              <div className="col-span-2 text-slate-800 font-medium">{profile.name}</div>

              <div className="col-span-1 text-slate-500 font-semibold">매장명</div>
              <div className="col-span-2 text-slate-800 font-medium">{profile.storeName || '-'}</div>

              <div className="col-span-1 text-slate-500 font-semibold">생년월일</div>
              <div className="col-span-2 text-slate-800 font-medium">{profile.birthday || '-'}</div>

              <div className="col-span-1 text-slate-500 font-semibold">핸드폰 번호</div>
              <div className="col-span-2 text-slate-800 font-medium">{profile.phone}</div>

              <div className="col-span-1 text-slate-500 font-semibold">상태</div>
              <div className="col-span-2">{getStatusLabel(profile.status)}</div>
            </div>

            <div className="pt-8 border-t border-slate-100 flex justify-center">
              <button onClick={() => setIsEditing(true)} className="px-10 py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-900 transition-colors shadow-md">
                정보 수정하기
              </button>
            </div>
          </div>
        ) : (
          
          <form onSubmit={handleProfileSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div>
               
                <label className="block text-xs font-semibold text-slate-500 mb-1">이메일 (수정 불가)</label>
                <div className="px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-slate-500 text-sm cursor-not-allowed">
                  {profile.loginId}
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">상태 (수정 불가)</label>
                <div className="px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-slate-500 text-sm cursor-not-allowed">
                  {profile.status === 'ACTIVE' ? '재직 중' : profile.status}
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">이름</label>
              <input type="text" name="name" value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} className="w-full px-4 py-2.5 bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none text-sm" />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">매장명</label>
              <input type="text" name="storeName" value={formData.storeName} onChange={(e) => setFormData({...formData, storeName: e.target.value})} className="w-full px-4 py-2.5 bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none text-sm" />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">핸드폰 번호</label>
              <input type="text" name="phone" value={formData.phone} onChange={(e) => setFormData({...formData, phone: e.target.value})} placeholder="010-0000-0000" className="w-full px-4 py-2.5 bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none text-sm" />
            </div>

            <div className="pt-8 border-t border-slate-100 flex justify-center gap-3">
              <button type="button" onClick={() => setIsEditing(false)} className="px-8 py-3 bg-white border border-slate-300 text-slate-600 font-bold rounded-xl hover:bg-slate-50" disabled={submitting}>
                취소
              </button>
              <button type="submit" className="flex items-center gap-2 px-8 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700" disabled={submitting}>
                {submitting ? <Spinner size="sm" /> : '수정 완료'}
              </button>
            </div>
          </form>
        )}
      </div>

  
      {pwModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-[2px]" onClick={() => setPwModalOpen(false)} />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
            <h3 className="text-lg font-bold text-slate-800 mb-4">비밀번호 변경</h3>
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">현재 비밀번호</label>
                <input type="password" value={pwData.password} onChange={(e) => setPwData({...pwData, password: e.target.value})} className="w-full px-4 py-2.5 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500" required />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">새 비밀번호</label>
                <input type="password" value={pwData.newPassword} onChange={(e) => setPwData({...pwData, newPassword: e.target.value})} className="w-full px-4 py-2.5 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500" required />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">새 비밀번호 확인</label>
                <input type="password" value={pwData.confirmPassword} onChange={(e) => setPwData({...pwData, confirmPassword: e.target.value})} className="w-full px-4 py-2.5 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500" required />
              </div>
              <div className="pt-2 flex gap-2">
                <button type="button" onClick={() => setPwModalOpen(false)} className="flex-1 py-2.5 text-sm bg-slate-100 font-semibold text-slate-600 rounded-xl">취소</button>
                <button type="submit" disabled={submitting} className="flex-1 py-2.5 text-sm bg-slate-800 text-white font-bold rounded-xl">{submitting ? '처리중...' : '변경하기'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}