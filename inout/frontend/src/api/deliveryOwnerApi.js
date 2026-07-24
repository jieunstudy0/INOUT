import apiClient from './apiClient';

export const getOwnerDeliveryList = async ({ status, page = 0, size = 10 }) => {
  const response = await apiClient.get('/owner/deliveries', {
    params: { status, page, size },
  });
  return response.data.body;
};
