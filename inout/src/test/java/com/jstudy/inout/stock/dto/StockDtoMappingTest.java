package com.jstudy.inout.stock.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.stock.dto.admin.StockAdminResponse;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.dto.emp.StockUserDetailResponse;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.entity.StockReceivingHistory;
import com.jstudy.inout.stock.entity.StockUsageHistory;

class StockDtoMappingTest {

    @Test
    @DisplayName("Item 엔티티를 StockAdminResponse로 변환 시 카테고리가 없으면 '미지정'으로 처리된다")
    void toStockAdminResponse_NullCategory() {
        // given
        Item item = Item.builder()
                .itemId(1L)
                .name("테스트상품")
                .category(null)
                .currentStock(10)
                .build();

        // when
        StockAdminResponse response = StockAdminResponse.from(item);

        // then
        assertThat(response.getCategoryName()).isEqualTo("미지정");
        assertThat(response.getName()).isEqualTo("테스트상품");
    }

    @Test
    @DisplayName("StockUserDetailResponse 상태 계산 - 재고가 0이면 '품절' 반환")
    void stockUserDetailResponse_Status_OutOfStock() {
        // given
        Item item = Item.builder().currentStock(0).minStockLevel(5).build();

        // when
        StockUserDetailResponse response = StockUserDetailResponse.from(item);

        // then
        assertThat(response.getStatus()).isEqualTo("품절");
    }

    @Test
    @DisplayName("StockUserDetailResponse 상태 계산 - 재고가 최소 재고 이하면 '저재고' 반환")
    void stockUserDetailResponse_Status_LowStock() {
        // given
        Item item = Item.builder().currentStock(4).minStockLevel(5).build();

        // when
        StockUserDetailResponse response = StockUserDetailResponse.from(item);

        // then
        assertThat(response.getStatus()).isEqualTo("저재고");
    }

    @Test
    @DisplayName("StockHistoryResponse 변환 - 입고 이력은 type이 '입고'로 설정된다")
    void toStockHistoryResponse_Receiving() {
        // given
        Item item = Item.builder().name("복사용지").build();
        User user = User.builder().name("김지은").build();
        StockReceivingHistory history = StockReceivingHistory.builder()
                .historyId(1L)
                .item(item)
                .user(user)
                .receivingQuantity(100)
                .resultStock(150)
                .processDate(LocalDateTime.now())
                .memo("정기 입고")
                .build();

        // when
        StockHistoryResponse response = StockHistoryResponse.from(history);

        // then
        assertThat(response.getType()).isEqualTo("입고");
        assertThat(response.getQuantity()).isEqualTo(100);
        assertThat(response.getWorkerName()).isEqualTo("김지은");
    }

    @Test
    @DisplayName("StockRegister DTO를 Item 엔티티로 변환한다 (minStockLevel 널 처리 검증)")
    void toEntity_MinStockLevelFallback() {
        // given
        ItemCategory category = ItemCategory.builder().categoryId(1).categoryName("문구").build();
        StockRegister registerDto = StockRegister.builder()
                .name("볼펜")
                .unitPrice(1000L)
                .minStockLevel(null) 
                .build();

        // when
        Item item = registerDto.toEntity(category);

        // then
        assertThat(item.getName()).isEqualTo("볼펜");
        assertThat(item.getMinStockLevel()).isEqualTo(0); 
        assertThat(item.getCurrentStock()).isEqualTo(0); 
    }
}