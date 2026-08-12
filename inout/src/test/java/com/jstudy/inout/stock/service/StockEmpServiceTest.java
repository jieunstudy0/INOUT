package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.emp.StockUserDetailResponse;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import com.jstudy.inout.stock.exception.NotEnoughStockException;

@ExtendWith(MockitoExtension.class)
class StockEmpServiceTest {

    @InjectMocks
    private StockEmpService stockEmpService;

    @Mock private ItemRepository itemRepository;
    @Mock private StockUsageHistoryRepository usageHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private StockReceivingHistoryRepository receivingHistoryRepository;

    @Test
    @DisplayName("재고 사용 성공 - 충분한 재고가 있을 때 수량이 차감되고 이력이 남는다")
    void useStock_Success() {
        // given
        Item item = Item.builder().itemId(1L).currentStock(50).deleted(false).build();
        User employee = User.builder().id(1L).build();

        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(employee));

        // when 
        Long usedItemId = stockEmpService.useStock(1L, 10, 1L, "매장 사용");

        // then
        assertThat(usedItemId).isEqualTo(1L);
        assertThat(item.getCurrentStock()).isEqualTo(40);
        verify(usageHistoryRepository).save(any());
    }

    @Test
    @DisplayName("재고 사용 실패 - 삭제된 상품(deleted=true)이면 예외가 발생한다")
    void useStock_Fail_DeletedItem() {
        // given
        Item deletedItem = Item.builder().itemId(1L).deleted(true).build(); 

        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(deletedItem));

        // when & then
        assertThatThrownBy(() -> stockEmpService.useStock(1L, 10, 1L, "사용"))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("사용 가능한 상품이 없습니다.");
    }

    @Test
    @DisplayName("재고 사용 실패 - 현재 재고보다 많은 수량을 요청하면 예외가 발생한다")
    void useStock_Fail_NotEnoughStock() {
        // given
        Item item = Item.builder().itemId(1L).currentStock(5).deleted(false).build(); 
        User employee = User.builder().id(1L).build();

        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(employee));

        // when & then 
        assertThatThrownBy(() -> stockEmpService.useStock(1L, 10, 1L, "사용"))
                .isInstanceOf(NotEnoughStockException.class); 
    }

    @Test
    @DisplayName("직원용 상품 상세 조회 성공")
    void getEmployeeStockDetail_Success() {
        // given
        ItemCategory category = ItemCategory.builder().categoryName("문구").build();
        Item item = Item.builder()
                .itemId(1L)
                .name("노트")
                .currentStock(100)
                .minStockLevel(0)
                .unitPrice(1000L)
                .category(category)
                .deleted(false)
                .build();

        given(itemRepository.findByItemIdAndDeletedFalse(1L)).willReturn(Optional.of(item));

        // when
        StockUserDetailResponse response = stockEmpService.getEmployeeStockDetail(1L);

        // then
        assertThat(response.getName()).isEqualTo("노트");
        assertThat(response.getCurrentStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("직원용 상품 상세 조회 실패 - 없는 상품이거나 삭제된 상품")
    void getEmployeeStockDetail_Fail_NotFound() {
        // given
        given(itemRepository.findByItemIdAndDeletedFalse(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stockEmpService.getEmployeeStockDetail(99L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("존재하지 않거나 삭제된 상품입니다.");
    }
}