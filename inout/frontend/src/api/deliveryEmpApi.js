import apiClient from './apiClient';

export const getEmpDeliveryList = async ({ status, page = 0, size = 10 }) => {
  // 직원의 배송 목록을 불러오는 백엔드 API (파라미터로 status 필터링 지원)
  const response = await apiClient.get('/emp/deliveries', {
    params: { status, page, size }
  });
  return response.data.body; 
};