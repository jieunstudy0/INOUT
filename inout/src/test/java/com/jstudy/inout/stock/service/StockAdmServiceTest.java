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
import com.jstudy.inout.stock.dto.admin.StockAdmRequest;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;

@ExtendWith(MockitoExtension.class)
class StockAdmServiceTest {

    @InjectMocks
    private StockAdmService stockAdmService;

    @Mock private ItemRepository itemRepository;
    @Mock private ItemCategoryRepository itemCategoryRepository;
    @Mock private StockReceivingHistoryRepository receivingHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private StockUsageHistoryRepository usageHistoryRepository;

    @Test
    @DisplayName("상품 등록 성공 - 정상 입력 시 DB에 저장된다")
    void registerStock_Success() {
        // given
    	StockRegister request = StockRegister.builder().name("새상품").categoryId(1).unitPrice(1000L).build();
        ItemCategory category = ItemCategory.builder()
                .categoryId(1) 
                .categoryName("문구류")
                .build();
        Item savedItem = Item.builder().itemId(100L).name("새상품").category(category).build();

        given(itemCategoryRepository.findById(1)).willReturn(Optional.of(category));
        given(itemRepository.existsByName("새상품")).willReturn(false);
        given(itemRepository.save(any(Item.class))).willReturn(savedItem);

        // when
        Long itemId = stockAdmService.registerStock(request);

        // then
        assertThat(itemId).isEqualTo(100L);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    @DisplayName("상품 등록 실패 - 이미 존재하는 상품명이면 예외가 발생한다")
    void registerStock_Fail_DuplicateName() {
        // given
        StockRegister request = StockRegister.builder().name("중복상품").categoryId(1).unitPrice(1000L).build();
        ItemCategory category = ItemCategory.builder()
                .categoryId(1) 
                .categoryName("문구류")
                .build();

        given(itemCategoryRepository.findById(1)).willReturn(Optional.of(category));
        given(itemRepository.existsByName("중복상품")).willReturn(true); // 중복 발생!

        // when & then
        assertThatThrownBy(() -> stockAdmService.registerStock(request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 등록된 상품명입니다.");
    }

    @Test
    @DisplayName("상품 삭제 성공 - 논리 삭제 처리(deleted=true)가 된다")
    void deleteStock_Success() {
        // given
        Item item = Item.builder().itemId(1L).deleted(false).build();
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        stockAdmService.deleteStock(1L);

        // then
        assertThat(item.getDeleted()).isTrue(); 
    }

    @Test
    @DisplayName("상품 삭제 실패 - 이미 삭제된 상품이면 예외가 발생한다")
    void deleteStock_Fail_AlreadyDeleted() {
        // given
        Item item = Item.builder().itemId(1L).deleted(true).build(); // 이미 삭제됨
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        

        // when & then
        assertThatThrownBy(() -> stockAdmService.deleteStock(1L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 삭제된 상품입니다.");
    }

    @Test
    @DisplayName("재고 입고 성공 - 재고가 증가하고 입고 이력이 저장된다")
    void receiveStock_Success() {
        // given
        Item item = Item.builder().itemId(1L).currentStock(10).build();
        User admin = User.builder().id(1L).build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // when
        stockAdmService.receiveStock(1L, 20, 1L, "신규 입고");

        // then
        assertThat(item.getCurrentStock()).isEqualTo(30); // 10 + 20
        verify(receivingHistoryRepository).save(any()); // 이력 저장 확인
    }

    @Test
    @DisplayName("재고 조정 성공 - 실제 재고가 더 많으면 차이만큼 입고 처리된다")
    void adjustStock_Success_Increase() {
        // given
        Item item = Item.builder().itemId(1L).currentStock(10).build();
        User admin = User.builder().id(1L).build();
        StockAdmRequest request = StockAdmRequest.builder()
                .itemId(1L)
                .actualStock(15)
                .reason("오차 발견")
                .build();

        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // when
        stockAdmService.adjustStock(1L, request);

        // then
        assertThat(item.getCurrentStock()).isEqualTo(15);
        verify(receivingHistoryRepository).save(any()); 
    }

    @Test
    @DisplayName("재고 조정 성공 - 실제 재고가 더 적으면 차이만큼 사용(차감) 처리된다")
    void adjustStock_Success_Decrease() {
        // given 
        Item item = Item.builder().itemId(1L).currentStock(10).build();
        User admin = User.builder().id(1L).build();
        StockAdmRequest request = StockAdmRequest.builder()
                .itemId(1L)
                .actualStock(7)
                .reason("")
                .build();

        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // when
        stockAdmService.adjustStock(1L, request);

        // then
        assertThat(item.getCurrentStock()).isEqualTo(7);
        verify(usageHistoryRepository).save(any());
    }
}