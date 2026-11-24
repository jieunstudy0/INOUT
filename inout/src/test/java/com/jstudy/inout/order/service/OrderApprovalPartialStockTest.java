package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.dto.BulkOrderRequest;
import com.jstudy.inout.order.dto.BulkOrderResponse;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.entity.DepositHistory;
import com.jstudy.inout.payment.entity.TransactionType;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 트러블슈팅 #1 회귀 테스트 — "일부 품목만 재고가 차감된 채 전액 환불되던 문제".
 *
 * 예전 코드는 한 반복문에서 품목별로 검사와 차감을 함께 했기 때문에, 마지막 품목에서 재고 부족이 나면
 * 앞 품목의 차감이 영속성 컨텍스트에 남은 채 환불·반려와 함께 커밋됐다.
 * 지금 코드는 모든 품목을 먼저 검사하고, 전부 통과했을 때만 차감한다.
 * 이 테스트는 그 순서가 다시 섞이면 실패한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderApprovalPartialStockTest {

    private static final long INITIAL_BALANCE = 100_000L;

    // 품목 A·B는 재고가 충분하고, 마지막 품목 C만 부족하다.
    private static final int STOCK_A = 10, QTY_A = 2;
    private static final int STOCK_B = 10, QTY_B = 3;
    private static final int STOCK_C = 1,  QTY_C = 5;
    private static final long PRICE = 1_000L;
    private static final long ORDER_TOTAL = PRICE * (QTY_A + QTY_B + QTY_C);

    @MockBean private MailComponent mailComponent;
    @MockBean private DeliveryService deliveryService;

    @Autowired private OrderApprovalTxService orderApprovalTxService;
    @Autowired private OrderAdmService orderAdmService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private OrderRequestRepository orderRequestRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private DepositAccountRepository depositAccountRepository;
    @Autowired private DepositHistoryRepository depositHistoryRepository;
    @Autowired private StockUsageHistoryRepository stockUsageHistoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Store store;
    private User admin;
    private User employee;
    private ItemCategory category;
    private Item itemA, itemB, itemC;
    private DepositAccount account;
    private OrderRequest order;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        tx.executeWithoutResult(s -> {
            store = storeRepository.save(Store.builder()
                    .name("부분차감매장-" + suffix)
                    .address("서울시 테스트구 " + suffix)
                    .build());
            admin = userRepository.save(user("admin-" + suffix, "관리자"));
            employee = userRepository.save(user("emp-" + suffix, "직원"));
            category = itemCategoryRepository.save(ItemCategory.builder()
                    .categoryName("부분차감카테고리-" + suffix)
                    .build());

            itemA = itemRepository.save(item("A-" + suffix, STOCK_A));
            itemB = itemRepository.save(item("B-" + suffix, STOCK_B));
            itemC = itemRepository.save(item("C-" + suffix, STOCK_C));

            // 점주 결제가 이미 끝난 상태(ORDERED)를 만든다: 잔액에서 주문 금액이 빠져 있다.
            account = depositAccountRepository.save(DepositAccount.builder()
                    .store(store)
                    .balance(INITIAL_BALANCE - ORDER_TOTAL)
                    .build());

            order = orderRequestRepository.save(OrderRequest.builder()
                    .requestUser(employee)
                    .status(OrderStatus.ORDERED)
                    .totalPrice(ORDER_TOTAL)
                    .requestDate(LocalDateTime.now())
                    .receiverName("수령인")
                    .receiverPhone("010-0000-0000")
                    .destinationAddress("서울시 테스트구 1번지")
                    .build());

            // 저장 순서 = 검사 순서. 부족한 품목 C를 마지막에 둬야 예전 버그가 재현된다.
            orderDetailRepository.save(detail(itemA, QTY_A));
            orderDetailRepository.save(detail(itemB, QTY_B));
            orderDetailRepository.save(detail(itemC, QTY_C));
        });
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(s -> depositHistoryRepository.deleteAll(
                depositHistoryRepository
                        .findByDepositAccountIdOrderByCreatedAtDesc(account.getId(), Pageable.unpaged())
                        .getContent()));
        tx.executeWithoutResult(s -> {
            for (Item item : List.of(itemA, itemB, itemC)) {
                stockUsageHistoryRepository.deleteAll(
                        stockUsageHistoryRepository.findAllByItem_ItemId(item.getItemId()));
            }
        });
        tx.executeWithoutResult(s -> {
            orderDetailRepository.deleteAll(orderDetailRepository.findAll().stream()
                    .filter(d -> d.getOrderRequest().getId().equals(order.getId()))
                    .toList());
            orderRequestRepository.deleteById(order.getId());
        });
        tx.executeWithoutResult(s -> {
            depositAccountRepository.deleteById(account.getId());
            itemRepository.deleteAllById(List.of(itemA.getItemId(), itemB.getItemId(), itemC.getItemId()));
            itemCategoryRepository.deleteById(category.getCategoryId());
            userRepository.deleteAllById(List.of(admin.getId(), employee.getId()));
            storeRepository.delete(store);
        });
    }

    @Test
    @DisplayName("[트러블슈팅 #1] 마지막 품목만 재고가 부족하면 앞 품목도 차감되지 않고, 전액 환불 후 반려된다")
    void lastItemShortage_noPartialDeduction_fullRefund() {
        boolean approved = orderApprovalTxService.processSingleOrderApproval(order.getId(), admin.getId());

        assertThat(approved).as("재고 부족이므로 승인되지 않아야 한다").isFalse();

        // 1) 재고: 세 품목 모두 처음 그대로여야 한다. (예전 버그에서는 A·B가 줄어 있었다)
        assertThat(stockOf(itemA)).as("품목 A 재고는 그대로").isEqualTo(STOCK_A);
        assertThat(stockOf(itemB)).as("품목 B 재고는 그대로").isEqualTo(STOCK_B);
        assertThat(stockOf(itemC)).as("품목 C 재고는 그대로").isEqualTo(STOCK_C);

        // 2) 재고 사용 이력도 한 건도 남지 않아야 한다.
        tx.executeWithoutResult(s -> {
            for (Item item : List.of(itemA, itemB, itemC)) {
                assertThat(stockUsageHistoryRepository.findAllByItem_ItemId(item.getItemId()))
                        .as("품목 %s의 사용 이력은 없어야 한다", item.getName())
                        .isEmpty();
            }
        });

        // 3) 주문과 모든 상세 품목이 반려 상태여야 한다.
        tx.executeWithoutResult(s -> {
            OrderRequest reloaded = orderRequestRepository.findByIdWithDetails(order.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(reloaded.getRejectReason()).startsWith("재고 부족 자동 취소");
            assertThat(reloaded.getOrderDetails())
                    .extracting(OrderDetail::getStatus)
                    .hasSize(3)
                    .containsOnly(OrderDetailStatus.REJECTED);
        });

        // 4) 예치금: 주문 금액 전액이 REFUND 이력 1건으로 돌아와 결제 전 잔액이 되어야 한다.
        tx.executeWithoutResult(s -> {
            List<DepositHistory> histories = new ArrayList<>(depositHistoryRepository
                    .findByDepositAccountIdOrderByCreatedAtDesc(account.getId(), Pageable.unpaged())
                    .getContent());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getType()).isEqualTo(TransactionType.REFUND);
            assertThat(histories.get(0).getAmount()).isEqualTo(ORDER_TOTAL);

            DepositAccount reloadedAccount = depositAccountRepository.findById(account.getId()).orElseThrow();
            assertThat(reloadedAccount.getBalance()).isEqualTo(INITIAL_BALANCE);
        });
    }

    @Test
    @DisplayName("[일괄 승인] 이미 반려·환불된 발주는 다시 승인되지 않고 배송도 만들지 않는다")
    void bulkApprove_rejectedOrder_staysRejected() {
        markOrder(OrderStatus.REJECTED, OrderDetailStatus.REJECTED);

        BulkOrderResponse result = orderAdmService.bulkApproveOrders(bulk(), admin.getId());

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertOrderUntouched(OrderStatus.REJECTED);
    }

    @Test
    @DisplayName("[일괄 승인] 점주 결제 전(직원 기안) 발주는 승인되지 않고 재고도 빠지지 않는다")
    void bulkApprove_unpaidOrder_notApproved() {
        markOrder(OrderStatus.REQUESTED, OrderDetailStatus.WAITING);

        BulkOrderResponse result = orderAdmService.bulkApproveOrders(bulk(), admin.getId());

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertOrderUntouched(OrderStatus.REQUESTED);
    }

    private void markOrder(OrderStatus status, OrderDetailStatus detailStatus) {
        tx.executeWithoutResult(s -> {
            OrderRequest o = orderRequestRepository.findByIdWithDetails(order.getId()).orElseThrow();
            o.updateStatus(status);
            o.getOrderDetails().forEach(d -> d.updateStatus(detailStatus));
        });
    }

    private BulkOrderRequest bulk() {
        BulkOrderRequest request = new BulkOrderRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "orderIds", List.of(order.getId()));
        return request;
    }

    private void assertOrderUntouched(OrderStatus expected) {
        tx.executeWithoutResult(s -> assertThat(orderRequestRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(expected));
        assertThat(stockOf(itemA)).isEqualTo(STOCK_A);
        assertThat(stockOf(itemB)).isEqualTo(STOCK_B);
        assertThat(stockOf(itemC)).isEqualTo(STOCK_C);
        verify(deliveryService, never()).createDeliveryIfAbsentForCompletedOrder(any());
        tx.executeWithoutResult(s -> assertThat(depositHistoryRepository
                .findByDepositAccountIdOrderByCreatedAtDesc(account.getId(), Pageable.unpaged()).getContent())
                .as("환불·차감 이력이 새로 생기지 않아야 한다").isEmpty());
    }

    private int stockOf(Item item) {
        return itemRepository.findById(item.getItemId()).orElseThrow().getCurrentStock();
    }

    private User user(String emailPrefix, String name) {
        return User.builder()
                .email(emailPrefix + "@inout-test.com")
                .password("encoded")
                .name(name)
                .phone("010-1111-2222")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .build();
    }

    private Item item(String name, int stock) {
        return Item.builder()
                .name("부분차감상품-" + name)
                .category(category)
                .unitPrice(PRICE)
                .currentStock(stock)
                .minStockLevel(0)
                .deleted(false)
                .build();
    }

    private OrderDetail detail(Item item, int quantity) {
        return OrderDetail.builder()
                .orderRequest(order)
                .item(item)
                .requestQuantity(quantity)
                .itemPriceSnapshot(PRICE)
                .build();
    }
}
