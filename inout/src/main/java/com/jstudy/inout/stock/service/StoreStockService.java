package com.jstudy.inout.stock.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.AdjustRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.CatalogItem;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.HistoryResponse;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.SettingsRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.StockResponse;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.UseRequest;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.entity.StoreStockHistory;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매장 재고 — 직원은 사용(차감)만, 점주는 조정·등록·설정까지 한다.
 * 매장은 항상 로그인한 사용자의 소속 매장으로 정하고, 요청에서 매장 ID를 받지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StoreStockService {

    private static final int HISTORY_LIMIT = 50;

    private final StoreStockRepository storeStockRepository;
    private final StoreStockHistoryRepository historyRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public List<StockResponse> getStoreStocks(Long userId) {
        Long storeId = storeIdOf(loadUser(userId));
        return storeStockRepository.findAllByStoreWithItem(storeId).stream()
                .map(StockResponse::from)
                .toList();
    }

    /** 점주가 매장 재고에 등록할 본사 상품 목록 (이미 등록된 상품은 표시만 한다) */
    public List<CatalogItem> getCatalog(Long ownerId) {
        Long storeId = storeIdOf(loadUser(ownerId));
        Set<Long> registered = storeStockRepository.findAllByStoreWithItem(storeId).stream()
                .map(s -> s.getItem().getItemId())
                .collect(Collectors.toSet());
        return itemRepository.findAllByDeletedFalse().stream()
                .map(i -> CatalogItem.builder()
                        .itemId(i.getItemId())
                        .itemName(i.getName())
                        .categoryName(i.getCategory() != null ? i.getCategory().getCategoryName() : "미지정")
                        .unitDescription(i.getUnitDescription())
                        .imageUrl(i.getImageUrl())
                        .registered(registered.contains(i.getItemId()))
                        .build())
                .toList();
    }

    public StockResponse getStoreStock(Long userId, Long storeStockId) {
        StoreStock stock = storeStockRepository.findById(storeStockId)
                .orElseThrow(() -> new InoutException(ErrorCode.STORE_STOCK_NOT_FOUND));
        assertSameStore(loadUser(userId), stock);
        return StockResponse.from(stock);
    }

    public List<HistoryResponse> getHistory(Long userId, Long storeStockId) {
        StoreStock stock = storeStockRepository.findById(storeStockId)
                .orElseThrow(() -> new InoutException(ErrorCode.STORE_STOCK_NOT_FOUND));
        assertSameStore(loadUser(userId), stock);
        return historyRepository.findRecentByStoreStock(storeStockId, PageRequest.of(0, HISTORY_LIMIT)).stream()
                .map(HistoryResponse::from)
                .toList();
    }

    /** 직원 사용 처리: 소속 매장 재고에서 차감한다. */
    @Transactional
    public void use(Long userId, UseRequest request) {
        User user = loadUser(userId);
        Long storeId = storeIdOf(user);
        StoreStock stock = storeStockRepository.findByStoreAndItemWithLock(storeId, request.getItemId())
                .orElseThrow(() -> new InoutException(ErrorCode.STORE_STOCK_NOT_FOUND,
                        "매장에 등록되지 않은 상품입니다. 점주에게 등록을 요청해 주세요."));

        stock.decrease(request.getQuantity());
        historyRepository.save(StoreStockHistory.builder()
                .storeStock(stock).user(user).type(StoreStockChangeType.USE)
                .changeQty(-request.getQuantity())
                .memo(request.getMemo())
                .build());
    }

    /**
     * 점주 조정: 실제 수량으로 맞춘다 (폐기·파손·매장 자체 구매·처음 등록).
     * 매장에 없던 상품이면 행을 새로 만든다. 차이가 없으면 아무것도 남기지 않는다.
     */
    @Transactional
    public void adjust(Long ownerId, AdjustRequest request) {
        User owner = loadUser(ownerId);
        Long storeId = storeIdOf(owner);

        StoreStock stock = storeStockRepository.findByStoreAndItemWithLock(storeId, request.getItemId())
                .orElseGet(() -> {
                    Item item = itemRepository.findByItemIdAndDeletedFalse(request.getItemId())
                            .orElseThrow(() -> new InoutException(ErrorCode.ITEM_NOT_FOUND));
                    return storeStockRepository.save(StoreStock.create(owner.getStore(), item));
                });

        int diff = request.getActualQuantity() - stock.getQuantity();
        if (diff == 0) {
            return;
        }
        if (diff > 0) {
            stock.increase(diff);
        } else {
            stock.decrease(-diff);
        }
        historyRepository.save(StoreStockHistory.builder()
                .storeStock(stock).user(owner).type(StoreStockChangeType.ADJUST)
                .changeQty(diff)
                .memo("[조정] " + request.getReason().trim())
                .build());
        log.info("[매장 재고] 점주 조정: storeId={}, itemId={}, diff={}", storeId, request.getItemId(), diff);
    }

    @Transactional
    public void updateSettings(Long ownerId, Long storeStockId, SettingsRequest request) {
        StoreStock stock = storeStockRepository.findById(storeStockId)
                .orElseThrow(() -> new InoutException(ErrorCode.STORE_STOCK_NOT_FOUND));
        assertSameStore(loadUser(ownerId), stock);
        stock.changeSettings(request.getMinStockLevel(), blankToNull(request.getLocation()));
    }

    // ──────────────────────────────────────────────────────────────────

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    static Long storeIdOf(User user) {
        if (user.getStore() == null) {
            throw new InoutException(ErrorCode.STORE_REQUIRED);
        }
        return user.getStore().getId();
    }

    static void assertSameStore(User user, StoreStock stock) {
        if (!storeIdOf(user).equals(stock.getStore().getId())) {
            throw new InoutException(ErrorCode.CROSS_STORE_FORBIDDEN);
        }
    }
}
