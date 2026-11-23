import client, { unwrap } from './apiClient';

export function getNotifications(page = 0, size = 20) {
  return unwrap(client.get('/notifications', { errorContext: '알림을 불러오지 못했습니다.', params: { page, size } }));
}

/** 헤더 배지용. 실패해도 화면에 오류를 띄우지 않는다 (주기적으로 다시 부른다). */
export function getUnreadNotificationCount() {
  return unwrap(client.get('/notifications/unread-count', { silent: true }));
}

export function markNotificationRead(notificationId) {
  return unwrap(client.patch(`/notifications/${notificationId}/read`));
}

export function markAllNotificationsRead() {
  return unwrap(client.patch('/notifications/read-all'));
}
