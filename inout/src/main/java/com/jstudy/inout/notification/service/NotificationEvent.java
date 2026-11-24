package com.jstudy.inout.notification.service;

import java.util.List;
import com.jstudy.inout.notification.entity.NotificationType;

/**
 * 알림을 보내 달라는 이벤트. 업무 트랜잭션 안에서 발행하고, 커밋된 뒤에만 알림으로 저장된다.
 *
 * @param actorId 이 일을 한 사람. 받는 사람 목록에 있어도 자기 자신에게는 보내지 않는다 (null이면 제외 없음).
 */
public record NotificationEvent(NotificationType type, String title, String message, Long actorId, List<Target> targets) {

    /** 받는 사람 지정. 같은 사람이 여러 대상에 겹치면 먼저 나온 대상의 링크로 한 번만 보낸다. */
    public record Target(Kind kind, Long id, String link) {

        public static Target user(Long userId, String link) {
            return new Target(Kind.USER, userId, link);
        }

        public static Target storeOwners(Long storeId, String link) {
            return new Target(Kind.STORE_OWNERS, storeId, link);
        }

        public static Target storeEmployees(Long storeId, String link) {
            return new Target(Kind.STORE_EMPLOYEES, storeId, link);
        }

        public static Target admins(String link) {
            return new Target(Kind.ADMINS, null, link);
        }
    }

    public enum Kind {
        USER, STORE_OWNERS, STORE_EMPLOYEES, ADMINS
    }
}
