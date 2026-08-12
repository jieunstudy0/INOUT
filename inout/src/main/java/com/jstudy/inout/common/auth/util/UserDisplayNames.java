package com.jstudy.inout.common.auth.util;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;

/**
 * 퇴사자 Soft-delete 정책에 맞춘 표시용 이름·매장명.
 * 과거 발주/문의 FK는 유지하되, UI에는 "(퇴사자)"를 병기한다.
 */
public final class UserDisplayNames {

    public static final String RESIGNED_SUFFIX = "(퇴사자)";
    public static final String NO_STORE_LABEL = "본점 (소속 없음)";

    private UserDisplayNames() {}

    public static String displayName(User user) {
        if (user == null) {
            return "-";
        }
        String name = user.getName() != null && !user.getName().isBlank() ? user.getName().trim() : "-";
        if (user.getStatus() == UserStatus.RESIGNED && !name.endsWith(RESIGNED_SUFFIX)) {
            return name + RESIGNED_SUFFIX;
        }
        return name;
    }

    public static String storeName(User user) {
        if (user == null || user.getStore() == null) {
            return NO_STORE_LABEL;
        }
        String name = user.getStore().getName();
        return name != null && !name.isBlank() ? name : NO_STORE_LABEL;
    }

    public static String storeNameOr(User user, String fallback) {
        if (user == null || user.getStore() == null) {
            return fallback != null ? fallback : NO_STORE_LABEL;
        }
        String name = user.getStore().getName();
        return name != null && !name.isBlank() ? name : (fallback != null ? fallback : NO_STORE_LABEL);
    }
}
