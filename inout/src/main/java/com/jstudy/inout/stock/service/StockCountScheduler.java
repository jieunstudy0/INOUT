package com.jstudy.inout.stock.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매일 아침(기본 9시) 매장마다 그날의 재고 실사표를 만든다.
 * 브라우저는 서버가 시키는 대로 자동 인쇄할 수 없어서, 서버는 실사표를 준비해 두고
 * 직원이 화면에서 [영수증 출력]을 눌러 인쇄한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "stock-count.scheduler.enabled", havingValue = "true")
public class StockCountScheduler {

    private final StockCountService stockCountService;

    @Scheduled(cron = "${stock-count.scheduler.cron:0 0 9 * * ?}", zone = "Asia/Seoul")
    public void createDailySheets() {
        int created = 0;
        for (Long storeId : stockCountService.storeIdsWithStock()) {
            // 매장별 경계: 한 매장이 실패해도 다른 매장 실사표는 계속 만든다.
            try {
                stockCountService.createSheetForStore(storeId, null);
                created++;
            } catch (InoutException e) {
                if (e.getCode() != ErrorCode.STOCK_COUNT_NO_TARGET) {
                    log.warn("[재고 실사 스케줄러] storeId={} 처리 중단: {} ({})", storeId, e.getMessage(), e.getResultCode());
                }
            } catch (Exception e) {
                log.error("[재고 실사 스케줄러] storeId={} 예상치 못한 오류", storeId, e);
            }
        }
        log.info("[재고 실사 스케줄러] 실사표 {}장 생성", created);
    }
}
