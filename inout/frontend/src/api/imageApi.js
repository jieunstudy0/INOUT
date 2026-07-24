
import client from './apiClient.js'; 
import { Toast } from '../utils/toast.js';

/**
 * 이미지를 백엔드 서버로 업로드하는 함수
 * @param {File} file - 업로드할 이미지 파일 객체
 * @returns {Promise<{ imageUrl: string }>} 백엔드에서 반환한 업로드된 이미지의 URL
 */
export const uploadImage = async (file) => {
  if (!file) return null;

  const formData = new FormData();
  formData.append('file', file); 

  try {
    const response = await client.post('/admin/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data', 
      },
    });

    const resultData = response.data.body;
    return resultData; 
    
  } catch (error) {
    console.error('이미지 업로드 실패:', error);
    Toast.error('이미지 업로드에 실패했습니다. 파일 용량이나 형식을 확인해주세요.');
    throw error;
  }
};