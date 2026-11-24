package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.order.service.OrderAdmService;
import com.jstudy.inout.order.service.OrderApprovalTxService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

/**
 * 본사 재고 수량을 바꾸는 메서드는 저재고 알림·대시보드 캐시를 비워야 한다.
 *
 * 예전에는 입고(receiveStock)만 비워서, 발주 승인·재고 조정·안전재고 변경·상품 삭제 뒤에도
 * 저재고 알림과 대시보드가 최대 3~5분 동안 예전 숫자를 보여 줬다.
 * 테스트 환경에는 Redis가 없어 캐시가 실제로 동작하지 않으므로, 메서드에 붙은 비우기 설정을 확인한다.
 */
class StockCacheEvictionTest {

    @ParameterizedTest(name = "{0}.{1}")
    @CsvSource({
            "StockAdmService, receiveStock",
            "StockAdmService, adjustStock",
            "StockAdmService, updateStock",
            "StockAdmService, deleteStock",
            "OrderApprovalTxService, processSingleOrderApproval",
            "OrderAdmService, processOrderItems",
    })
    void stockChangingMethod_evictsStockCaches(String className, String methodName) {
        List<String> evicted = evictedCaches(find(className, methodName));

        assertThat(evicted).contains(CacheConfig.STOCK_ALERTS, CacheConfig.DASHBOARD_SUMMARY);
    }

    private static Method find(String className, String methodName) {
        Class<?> type = switch (className) {
            case "StockAdmService" -> StockAdmService.class;
            case "OrderApprovalTxService" -> OrderApprovalTxService.class;
            case "OrderAdmService" -> OrderAdmService.class;
            default -> throw new IllegalArgumentException(className);
        };
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().orElseThrow();
    }

    private static List<String> evictedCaches(Method method) {
        List<CacheEvict> evicts = new ArrayList<>();
        CacheEvict single = method.getAnnotation(CacheEvict.class);
        if (single != null) evicts.add(single);
        Caching caching = method.getAnnotation(Caching.class);
        if (caching != null) evicts.addAll(Arrays.asList(caching.evict()));
        return evicts.stream().flatMap(e -> Arrays.stream(e.value())).toList();
    }
}
