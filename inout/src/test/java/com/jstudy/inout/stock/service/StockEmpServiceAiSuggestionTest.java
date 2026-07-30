package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.stock.dto.emp.AiStockSuggestionResponse;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI 스마트 발주 추천(GET /api/emp/stocks/ai-suggestions)은 Redis 캐시를 사용하지 않으므로
 * Redis 미기동과 무관하게 DB 기반 휴리스틱 결과를 반환해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class StockEmpServiceAiSuggestionTest {

    private ItemRepository itemRepository;
    private StockReceivingHistoryRepository receivingHistoryRepository;
    private StockUsageHistoryRepository usageHistoryRepository;
    private UserRepository userRepository;
    private StockEmpService stockEmpService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        receivingHistoryRepository = mock(StockReceivingHistoryRepository.class);
        usageHistoryRepository = mock(StockUsageHistoryRepository.class);
        userRepository = mock(UserRepository.class);
        stockEmpService = new StockEmpService(
                itemRepository, usageHistoryRepository, userRepository, receivingHistoryRepository);
    }

    @Test
    @DisplayName("Redis 없이도 AI 발주 추천 목록을 정상 반환한다")
    void getAiStockSuggestions_returnsDataWithoutRedis() {
        Item lowStock = Item.builder()
                .itemId(1L)
                .name("생수 2L")
                .currentStock(0)
                .minStockLevel(10)
                .unitPrice(1000L)
                .deleted(false)
                .build();
        given(itemRepository.findAllByDeletedFalse()).willReturn(List.of(lowStock));
        given(usageHistoryRepository.sumRecentSalesByItem(any()))
                .willReturn(Collections.emptyList());

        List<AiStockSuggestionResponse> result = stockEmpService.getAiStockSuggestions(8);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(1L);
        assertThat(result.get(0).getRecommendQty()).isGreaterThan(0);
        assertThat(result.get(0).getReason()).isNotBlank();
    }
}
