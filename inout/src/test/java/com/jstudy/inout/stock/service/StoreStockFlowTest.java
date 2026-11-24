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
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.AdjustRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveLine;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.SettingsRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.UseRequest;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import com.jstudy.inout.stock.testsupport.StoreStockFixture;
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
 * 매장 재고: 직원 사용, 점주 조정, 배송 수령 확인.
 * 공통 검사: 매장 재고만 바뀌고 본사 재고(Item.currentStock)는 그대로여야 하며, 다른 매장 데이터는 건드릴 수 없다.
 */
@SpringBootTest
@ActiveProfiles("test")
class StoreStockFlowTest {

    @MockBean private MailComponent mailComponent;

    @Autowired private StoreStockService storeStockService;
    @Autowired private StoreReceiptService storeReceiptService;
    @Autowired private StoreStockRepository storeStockRepository;
    @Autowired private StoreStockHistoryRepository historyRepository;
    @Autowired private PlatformTransactionManager tm;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private StockCountSheetRepository sheetRepository;
    @Autowired private OrderRequestRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private DeliveryRepository deliveryRepository;

    private StoreStockFixture f;
    private Store store;
    private User owner;
    private User employee;
    private User otherEmployee;

    @BeforeEach
    void setUp() {
        f = new StoreStockFixture(tm, storeRepository, userRepository, categoryRepository, itemRepository,
                storeStockRepository, historyRepository, sheetRepository, orderRepository, orderDetailRepository,
                deliveryRepository);
        store = f.store("흐름매장");
        Store otherStore = f.store("흐름다른매장");
        owner = f.user(store, "flow-owner");
        employee = f.user(store, "flow-emp");
        otherEmployee = f.user(otherStore, "flow-other-emp");
    }

    @AfterEach
    void tearDown() {
        f.cleanUp();
    }

    // ── 직원 사용 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("직원 사용은 매장 재고만 줄이고 이력을 남기며, 본사 재고는 그대로다")
    void use_decreasesStoreStockOnly() {
        Item item = f.item("사용", 100);
        StoreStock stock = f.storeStock(store, item, 10, 2, null);

        storeStockService.use(employee.getId(), new UseRequest(item.getItemId(), 3, "음료 제조"));

        assertThat(f.qtyOf(stock)).isEqualTo(7);
        assertThat(f.hqStockOf(item)).isEqualTo(100);
        f.tx.executeWithoutResult(t -> assertThat(historyRepository.findAllByStoreStock_Id(stock.getId()))
                .singleElement()
                .satisfies(h -> {
                    assertThat(h.getType()).isEqualTo(StoreStockChangeType.USE);
                    assertThat(h.getChangeQty()).isEqualTo(-3);
                    assertThat(h.getResultQty()).isEqualTo(7);
                }));
    }

    @Test
    @DisplayName("매장 재고보다 많이 사용하면 STORE_STOCK_SHORTAGE이고 아무것도 바뀌지 않는다")
    void use_moreThanStock_rejected() {
        Item item = f.item("부족", 100);
        StoreStock stock = f.storeStock(store, item, 2, 0, null);

        assertThatThrownBy(() -> storeStockService.use(employee.getId(), new UseRequest(item.getItemId(), 3, null)))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.STORE_STOCK_SHORTAGE);
        assertThat(f.qtyOf(stock)).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 매장에만 있는 상품은 사용할 수 없다 (내 매장에 없는 상품 = STORE_STOCK_NOT_FOUND)")
    void use_otherStoresItem_notFound() {
        Item item = f.item("남의상품", 100);
        StoreStock stock = f.storeStock(store, item, 10, 0, null);

        assertThatThrownBy(() -> storeStockService.use(otherEmployee.getId(), new UseRequest(item.getItemId(), 1, null)))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.STORE_STOCK_NOT_FOUND);
        assertThat(f.qtyOf(stock)).isEqualTo(10);
    }

    // ── 점주 조정 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("점주 조정은 매장에 없던 상품이면 새로 등록하고, 실제 수량으로 맞추며 사유를 남긴다")
    void adjust_createsAndSets() {
        Item item = f.item("처음등록", 100);

        storeStockService.adjust(owner.getId(), new AdjustRequest(item.getItemId(), 12, "처음 재고 등록"));

        StoreStock created = storeStockRepository.findByStore_IdAndItem_ItemId(store.getId(), item.getItemId()).orElseThrow();
        f.track(created);
        assertThat(created.getQuantity()).isEqualTo(12);
        f.tx.executeWithoutResult(t -> assertThat(historyRepository.findAllByStoreStock_Id(created.getId()))
                .singleElement()
                .satisfies(h -> {
                    assertThat(h.getType()).isEqualTo(StoreStockChangeType.ADJUST);
                    assertThat(h.getChangeQty()).isEqualTo(12);
                    assertThat(h.getMemo()).contains("처음 재고 등록");
                }));
        assertThat(f.hqStockOf(item)).isEqualTo(100);
    }

    @Test
    @DisplayName("다른 매장 사용자는 매장 재고 설정을 바꿀 수 없다")
    void settings_otherStore_forbidden() {
        StoreStock stock = f.storeStock(store, f.item("설정", 10), 3, 1, "A-01");

        assertThatThrownBy(() -> storeStockService.updateSettings(otherEmployee.getId(), stock.getId(),
                new SettingsRequest(9, "Z-99")))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
    }

    // ── 배송 수령 확인 ───────────────────────────────────────────────

    @Test
    @DisplayName("수령 확인: 받은 수량만큼 매장 재고가 늘고, 덜 받은 수량이 기록되며, 배송은 완료·수령 처리된다")
    void receive_increasesByReceivedQty() {
        Item a = f.item("수령A", 100);
        Item b = f.item("수령B", 100);
        StoreStock existing = f.storeStock(store, a, 2, 0, null);
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(a, b), List.of(5, 4));
        List<OrderDetail> details = f.detailsOf(order);

        var result = storeReceiptService.receive(employee.getId(), order.getId(), new ReceiveRequest(List.of(
                new ReceiveLine(details.get(0).getOrderDetailId(), 5),
                new ReceiveLine(details.get(1).getOrderDetailId(), 3))));

        assertThat(result.getShortItemCount()).isEqualTo(1);
        assertThat(f.qtyOf(existing)).isEqualTo(7);
        StoreStock createdB = storeStockRepository.findByStore_IdAndItem_ItemId(store.getId(), b.getItemId()).orElseThrow();
        f.track(createdB);
        assertThat(createdB.getQuantity()).as("매장에 없던 상품은 수령 때 새로 생긴다").isEqualTo(3);
        assertThat(f.detailsOf(order)).extracting(OrderDetail::getReceivedQuantity).containsExactly(5, 3);

        Delivery delivery = f.deliveryOf(order);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
        assertThat(delivery.isReceived()).isTrue();
        assertThat(f.hqStockOf(a)).as("본사 재고는 발주 승인 때 이미 줄었으므로 수령으로 바뀌지 않는다").isEqualTo(100);
    }

    @Test
    @DisplayName("같은 배송을 두 번 수령 확인하면 ALREADY_RECEIVED이고 재고는 한 번만 늘어난다")
    void receive_twice_once() {
        Item a = f.item("두번수령", 100);
        StoreStock stock = f.storeStock(store, a, 0, 0, null);
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(a), List.of(4));
        ReceiveRequest req = new ReceiveRequest(List.of(new ReceiveLine(f.detailsOf(order).get(0).getOrderDetailId(), 4)));

        storeReceiptService.receive(employee.getId(), order.getId(), req);
        assertThatThrownBy(() -> storeReceiptService.receive(owner.getId(), order.getId(), req))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.ALREADY_RECEIVED);

        assertThat(f.qtyOf(stock)).isEqualTo(4);
    }

    @Test
    @DisplayName("한 품목이라도 수령 수량이 발주보다 많으면 거절되고, 앞 품목 재고도 늘지 않는다")
    void receive_oneInvalid_nothingApplied() {
        Item a = f.item("정상수령", 100);
        Item b = f.item("초과수령", 100);
        StoreStock stockA = f.storeStock(store, a, 1, 0, null);
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(a, b), List.of(2, 2));
        List<OrderDetail> details = f.detailsOf(order);

        assertThatThrownBy(() -> storeReceiptService.receive(employee.getId(), order.getId(), new ReceiveRequest(List.of(
                new ReceiveLine(details.get(0).getOrderDetailId(), 2),
                new ReceiveLine(details.get(1).getOrderDetailId(), 3)))))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_RECEIVED_QUANTITY);

        assertThat(f.qtyOf(stockA)).isEqualTo(1);
        assertThat(f.deliveryOf(order).isReceived()).isFalse();
    }

    @Test
    @DisplayName("다른 매장의 배송은 수령 확인할 수 없다")
    void receive_otherStore_forbidden() {
        Item a = f.item("남의배송", 100);
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(a), List.of(1));
        ReceiveRequest req = new ReceiveRequest(List.of(new ReceiveLine(f.detailsOf(order).get(0).getOrderDetailId(), 1)));

        assertThatThrownBy(() -> storeReceiptService.receive(otherEmployee.getId(), order.getId(), req))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
    }

    @Test
    @DisplayName("아직 출발하지 않은 배송(READY)은 수령 확인할 수 없다")
    void receive_ready_rejected() {
        Item a = f.item("미출발", 100);
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.READY, List.of(a), List.of(1));
        ReceiveRequest req = new ReceiveRequest(List.of(new ReceiveLine(f.detailsOf(order).get(0).getOrderDetailId(), 1)));

        assertThatThrownBy(() -> storeReceiptService.receive(employee.getId(), order.getId(), req))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_DELIVERY_STATUS);
    }

    @Test
    @DisplayName("수령 대기 목록에는 소속 매장의 배송 중 배송만 나온다")
    void pendingReceipts_ownStoreOnly() {
        Item a = f.item("대기", 100);
        OrderRequest mine = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(a), List.of(1));
        OrderRequest others = f.shippedOrder(otherEmployee, DeliveryStatus.SHIPPING, List.of(a), List.of(1));

        var pending = storeReceiptService.getPendingReceipts(employee.getId());

        assertThat(pending).extracting(p -> p.getOrderId()).contains(mine.getId()).doesNotContain(others.getId());
    }
}
