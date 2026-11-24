import { useRef, useState } from 'react';
import AuthImage from './AuthImage';
import { Toast } from '../../utils/toast';
import { validateImageFile } from '../../utils/images';

/**
 * 사진 입력칸 — 미리보기 + [사진 선택]. 고르면 바로 업로드하고 받은 경로를 onChange로 넘긴다.
 * (저장 버튼을 눌러야 실제 상품·직원 정보에 반영된다)
 *
 * shape: 'rect'(상품, 4:3) | 'circle'(직원)
 * upload: (file) => Promise<string>  업로드 후 경로를 돌려주는 함수
 */
export default function PhotoField({ value, onChange, upload, shape = 'rect', placeholder, label = '사진' }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);

  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const error = validateImageFile(file);
    if (error) { Toast.warning(error); return; }
    setUploading(true);
    try {
      const url = await upload(file);
      onChange(url);
    } catch {
      /* interceptor */
    } finally {
      setUploading(false);
    }
  };

  const frame = shape === 'circle'
    ? 'w-32 h-32 rounded-full'
    : 'w-full aspect-[4/3] rounded-[10px]';

  return (
    <div>
      <div className={`${frame} border border-slate-200 bg-slate-50 overflow-hidden flex items-center justify-center`}>
        {value ? (
          <AuthImage src={value} alt={label} className="w-full h-full object-cover" />
        ) : (
          placeholder || <span className="text-sm text-slate-400">사진 없음</span>
        )}
      </div>
      <div className={`flex gap-2 mt-3 ${shape === 'circle' ? 'justify-center' : ''}`}>
        <button type="button" onClick={() => inputRef.current?.click()} disabled={uploading}
          className="px-4 py-2 text-sm rounded-full border border-indigo-600 text-indigo-600 hover:bg-indigo-50 disabled:opacity-50">
          {uploading ? '올리는 중...' : value ? '사진 바꾸기' : '사진 올리기'}
        </button>
        {value && (
          <button type="button" onClick={() => onChange('')} className="px-3 py-2 text-sm text-slate-500 hover:text-rose-600">
            지우기
          </button>
        )}
      </div>
      <input ref={inputRef} type="file" accept="image/jpeg,image/png,image/gif,image/webp" className="hidden" onChange={pick} />
    </div>
  );
}
