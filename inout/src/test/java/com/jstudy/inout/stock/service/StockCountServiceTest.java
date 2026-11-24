package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.dto.count.StockCountLineResponse;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import com.jstudy.inout.stock.dto.count.StockCountSubmitRequest;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StockCountReason;
import com.jstudy.inout.stock.entity.StockCountStatus;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import com.jstudy.inout.stock.testsupport.StoreStockFixture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 매장 재고 실사 — 재고를 바꾸는 코드라 실패 경우를 중심으로 검사한다.
 * - 반영량은 "실재고 - 출력 시점 매장 재고"이고, 출력 이후의 사용을 덮어쓰지 않는다.
 * - 한 상품이라도 0 미만이 되면 아무것도 바뀌지 않는다(전부 검사 후 반영).
 * - 같은 실사표를 두 번 승인해도 한 번만 반영된다.
 * - 다른 매장의 실사표는 보거나 승인할 수 없다.
 */
@SpringBootTest
@ActiveProfiles("test")
class StockCountServiceTest {

    @MockBean private MailComponent mailComponent;

    @Autowired private StockCountService stockCountService;
    @Autowired private StockCountSheetRepository sheetRepository;
    @Autowired private StoreStockHistoryRepository historyRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private PlatformTransactionManager tm;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private StoreStockRepository storeStockRepository;
    @Autowired private OrderRequestRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private DeliveryRepository deliveryRepository;

    private StoreStockFixture f;
    private Store store;
    private Store otherStore;
    private User owner;
    private User employee;
    private User otherOwner;

    @BeforeEach
    void setUp() {
        f = new StoreStockFixture(tm, storeRepository, userRepository, categoryRepository, itemRepository,
                storeStockRepository, historyRepository, sheetRepository, orderRepository, orderDetailRepository,
                deliveryRepository);
        store = f.store("실사매장");
        otherStore = f.store("다른매장");
        owner = f.user(store, "count-owner");
        employee = f.user(store, "count-emp");
        otherOwner = f.user(otherStore, "count-other-owner");
    }

    @AfterEach
    void tearDown() {
        f.cleanUp();
    }

    // ── 만들기 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("실사표에는 소속 매장의 저재고·최근 사용 상품만 오르고, 위치 순서로 줄 번호가 매겨진다")
    void createSheet_picksOwnStoreLowAndRecentlyUsed() {
        StoreStock low = f.storeStock(store, f.item("저재고", 100), 2, 5, "B-02");
        StoreStock used = f.storeStock(store, f.item("최근사용", 100), 50, 5, "A-01");
        StoreStock normal = f.storeStock(store, f.item("정상", 100), 50, 5, "A-02");
        StoreStock otherLow = f.storeStock(otherStore, f.item("남의매장", 100), 0, 5, "A-00");
        f.useRecord(used, employee);

        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);
        StockCountSheetResponse sheet = stockCountService.getSheet(employee.getId(), sheetId);

        assertThat(sheet.getLines()).extracting(StockCountLineResponse::getStoreStockId)
                .containsExactly(used.getId(), low.getId())
                .doesNotContain(normal.getId(), otherLow.getId());
        assertThat(sheet.getLines().get(1).getReason()).isEqualTo(StockCountReason.LOW_STOCK);
        assertThat(sheet.getLines().get(1).getSystemQty()).isEqualTo(2);
        assertThat(sheet.getLines().get(0).getReason()).isEqualTo(StockCountReason.RECENT_USE);
    }

    @Test
    @DisplayName("다른 매장 사용자는 실사표를 조회할 수 없다 (CROSS_STORE_FORBIDDEN)")
    void getSheet_otherStore_forbidden() {
        f.storeStock(store, f.item("조회", 10), 0, 5, null);
        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);

        assertThatThrownBy(() -> stockCountService.getSheet(otherOwner.getId(), sheetId))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
    }

    // ── 제출 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("실재고를 하나도 입력하지 않고 제출하면 STOCK_COUNT_EMPTY")
    void submit_allBlank_rejected() {
        Long sheetId = sheetWith(f.storeStock(store, f.item("빈제출", 10), 3, 5, null));

        assertThatThrownBy(() -> submit(sheetId, (Integer) null))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.STOCK_COUNT_EMPTY);
        assertThat(statusOf(sheetId)).isEqualTo(StockCountStatus.OPEN);
    }

    // ── 승인 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("출력 후 사용이 있었어도, 승인은 (실재고 - 출력 시점 재고)만큼만 지금 매장 재고에 더한다")
    void approve_appliesDiffFromSnapshot_keepsLaterMovements() {
        Item item = f.item("출력후사용", 100);
        StoreStock stock = f.storeStock(store, item, 5, 5, null);   // 출력 시점 5개
        Long sheetId = sheetWith(stock);
        f.setQty(stock, 3);                                         // 출력 후 2개 사용 → 지금 3개
        submit(sheetId, 4);                                         // 실재고 4개 → 출력 시점보다 1개 적음

        var result = stockCountService.approve(sheetId, owner.getId());

        // 실재고(4)로 덮어쓰면 출력 후 사용한 2개가 되살아난다. 차이(-1)만 반영해 3 - 1 = 2가 맞다.
        assertThat(f.qtyOf(stock)).isEqualTo(2);
        assertThat(result.getDecreasedCount()).isEqualTo(1);
        assertThat(statusOf(sheetId)).isEqualTo(StockCountStatus.APPLIED);
        assertThat(f.hqStockOf(item)).as("본사 재고는 그대로").isEqualTo(100);
        f.tx.executeWithoutResult(t -> assertThat(historyRepository.findAllByStoreStock_Id(stock.getId()))
                .filteredOn(h -> h.getType() == StoreStockChangeType.COUNT)
                .singleElement()
                .satisfies(h -> {
                    assertThat(h.getChangeQty()).isEqualTo(-1);
                    assertThat(h.getResultQty()).isEqualTo(2);
                }));
    }

    @Test
    @DisplayName("한 상품이라도 반영 후 0 미만이 되면 승인이 거절되고, 다른 상품 재고도 이력도 바뀌지 않는다")
    void approve_oneNegative_nothingApplied() {
        // itemId 오름차순으로 처리되므로, 늘어나는 상품 A를 먼저 만들어 앞에 둔다.
        StoreStock a = f.storeStock(store, f.item("늘어남", 10), 5, 10, null);
        StoreStock b = f.storeStock(store, f.item("음수됨", 10), 5, 10, null);
        Long sheetId = sheetWith(a, b);
        f.setQty(b, 0);         // 출력 후 B를 전부 사용
        submit(sheetId, 8, 2);  // A: +3, B: -3 → B는 0 - 3 = -3

        assertThatThrownBy(() -> stockCountService.approve(sheetId, owner.getId()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.STOCK_COUNT_NEGATIVE_RESULT);

        assertThat(f.qtyOf(a)).as("A는 검사만 통과했을 뿐 반영되면 안 된다").isEqualTo(5);
        assertThat(f.qtyOf(b)).isEqualTo(0);
        f.tx.executeWithoutResult(t -> assertThat(historyRepository.findAllByStoreStock_Id(a.getId())).isEmpty());
        assertThat(statusOf(sheetId)).isEqualTo(StockCountStatus.SUBMITTED);
    }

    @Test
    @DisplayName("같은 실사표를 두 번 승인하면 두 번째는 STOCK_COUNT_INVALID_STATUS이고 재고는 한 번만 바뀐다")
    void approve_twice_appliedOnce() {
        StoreStock stock = f.storeStock(store, f.item("두번승인", 10), 5, 10, null);
        Long sheetId = sheetWith(stock);
        submit(sheetId, 7);

        stockCountService.approve(sheetId, owner.getId());
        assertThatThrownBy(() -> stockCountService.approve(sheetId, owner.getId()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.STOCK_COUNT_INVALID_STATUS);

        assertThat(f.qtyOf(stock)).isEqualTo(7);
    }

    @Test
    @DisplayName("다른 매장 점주는 승인할 수 없고, 재고도 바뀌지 않는다")
    void approve_otherStoreOwner_forbidden() {
        StoreStock stock = f.storeStock(store, f.item("남의승인", 10), 5, 10, null);
        Long sheetId = sheetWith(stock);
        submit(sheetId, 9);

        assertThatThrownBy(() -> stockCountService.approve(sheetId, otherOwner.getId()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
        assertThat(f.qtyOf(stock)).isEqualTo(5);
    }

    @Test
    @DisplayName("비워 둔 상품과 차이가 없는 상품은 재고·이력이 바뀌지 않는다")
    void approve_blankAndSame_untouched() {
        StoreStock same = f.storeStock(store, f.item("같음", 10), 4, 10, null);
        StoreStock blank = f.storeStock(store, f.item("안셈", 10), 4, 10, null);
        Long sheetId = sheetWith(same, blank);
        submit(sheetId, 4, null);

        var result = stockCountService.approve(sheetId, owner.getId());

        assertThat(result.getUnchangedCount()).isEqualTo(1);
        assertThat(f.qtyOf(same)).isEqualTo(4);
        assertThat(f.qtyOf(blank)).isEqualTo(4);
        f.tx.executeWithoutResult(t -> {
            assertThat(historyRepository.findAllByStoreStock_Id(same.getId())).isEmpty();
            assertThat(historyRepository.findAllByStoreStock_Id(blank.getId())).isEmpty();
        });
    }

    @Test
    @DisplayName("반려하면 작성 중으로 돌아가고 재고는 그대로이며, 다시 제출할 수 있다")
    void reject_returnsToOpen() {
        StoreStock stock = f.storeStock(store, f.item("반려", 10), 5, 10, null);
        Long sheetId = sheetWith(stock);
        submit(sheetId, 1);

        stockCountService.reject(sheetId, owner.getId(), "  1번 다시 세어 주세요  ");

        StockCountSheetResponse reloaded = stockCountService.getSheet(employee.getId(), sheetId);
        assertThat(reloaded.getStatus()).isEqualTo(StockCountStatus.OPEN);
        assertThat(reloaded.getRejectReason()).isEqualTo("1번 다시 세어 주세요");
        assertThat(f.qtyOf(stock)).isEqualTo(5);

        submit(sheetId, 5);
        assertThat(statusOf(sheetId)).isEqualTo(StockCountStatus.SUBMITTED);
    }

    // ── helpers ─────────────────────────────────────────────────────

    /** 이 테스트에서 만든 매장 재고만 오르도록, 매장에 다른 대상이 없는 상태에서 실사표를 만든다. */
    private Long sheetWith(StoreStock... stocks) {
        // 안전재고를 수량 이상으로 두었으므로 모두 저재고 대상이 된다.
        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);
        List<Long> expected = Arrays.stream(stocks).map(StoreStock::getId).toList();
        assertThat(stockCountService.getSheet(employee.getId(), sheetId).getLines())
                .extracting(StockCountLineResponse::getStoreStockId)
                .containsExactlyInAnyOrderElementsOf(expected);
        return sheetId;
    }

    /** counts는 실사표 줄 순서가 아니라 stocks 생성 순서(=storeStockId 순서)를 따른다. */
    private void submit(Long sheetId, Integer... counts) {
        List<StockCountLineResponse> lines = new ArrayList<>(stockCountService.getSheet(employee.getId(), sheetId).getLines());
        lines.sort((x, y) -> x.getStoreStockId().compareTo(y.getStoreStockId()));
        List<StockCountSubmitRequest.Line> body = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            body.add(new StockCountSubmitRequest.Line(lines.get(i).getLineId(), counts[i]));
        }
        stockCountService.submit(sheetId, employee.getId(), new StockCountSubmitRequest(body));
    }

    private StockCountStatus statusOf(Long sheetId) {
        return sheetRepository.findById(sheetId).orElseThrow().getStatus();
    }
}
