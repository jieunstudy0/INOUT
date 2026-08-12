package com.jstudy.inout.delivery.scheduler;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryScheduler {

    private final DeliveryRepository deliveryRepository;


    @Scheduled(cron = "0 0 9 * * *") 
    @Transactional
    public void autoCompleteOldDeliveries() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        log.info("배송 자동 완료 스케줄러 실행 (기준일시: {})", threshold);
        deliveryRepository.findByStatusAndShippedAtBefore(DeliveryStatus.SHIPPING, threshold)
            .forEach(d -> {
                d.completeDelivery(LocalDateTime.now());
                log.info("배송 ID {} 자동 완료 처리", d.getId());
            });
            
        log.info("배송 자동 완료 스케줄러 종료");
    }
}