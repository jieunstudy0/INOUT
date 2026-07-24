package com.jstudy.inout.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.order.service.OrderApprovalTxService;
import com.jstudy.inout.payment.dto.PaymentDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("test")
class DepositLedgerConcurrencyTest {

    private static final int  WORKER_COUNT    = 100;
    private static final long ITEM_PRICE      = 1_000L;
    private static final long INITIAL_DEPOSIT = 100_000L;

    @MockBean private MailComponent mailComponent;
    @MockBean private DeliveryService deliveryService;

    @Autowired private PaymentService paymentService;
    @Autowired private OrderApprovalTxService orderApprovalTxService;
    @Autowired private UserRepository           userRepository;
    @Autowired private StoreRepository          storeRepository;
    @Autowired private ItemRepository           itemRepository;
    @Autowired private ItemCategoryRepository   itemCategoryRepository;
    @Autowired private OrderRequestRepository   orderRequestRepository;
    @Autowired private OrderDetailRepository    orderDetailRepository;
    @Autowired private DepositAccountRepository depositAccountRepository;
    @Autowired private DepositHistoryRepository depositHistoryRepository;
    @Autowired private StockUsageHistoryRepository stockUsageHistoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private String              suffix;
    private Store               store;
    private User                admin;
    private Item                item;
    private ItemCategory        category;
    private List<User>          employees;
    private List<DepositAccount> depositAccounts;
    private List<OrderRequest>  orders;


    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(s -> {
            store = storeRepository.save(Store.builder()
                    .name("동시성매장-" + suffix)
                    .address("서울시 테스트구 " + suffix)
                    .build());

            admin = userRepository.save(User.builder()
                    .email("admin-" + suffix + "@inout-test.com")
                    .password("encoded")
                    .name("관리자-" + suffix)
                    .phone("010-9999-" + suffix.substring(0, 4))
                    .birthday(LocalDate.of(1980, 1, 1))
                    .store(store)
                    .build());

            category = itemCategoryRepository.save(ItemCategory.builder()
                    .categoryName("카테고리-" + suffix)
                    .build());

            item = itemRepository.save(Item.builder()
                    .name("인기상품-" + suffix)
                    .category(category)
                    .unitPrice(ITEM_PRICE)
                    .currentStock(WORKER_COUNT)
                    .minStockLevel(0)
                    .deleted(false)
                    .build());

            employees       = new ArrayList<>();
            depositAccounts = new ArrayList<>();
            orders          = new ArrayList<>();

            depositAccounts.add(depositAccountRepository.save(DepositAccount.builder()
                    .store(store)
                    .balance(INITIAL_DEPOSIT * WORKER_COUNT)
                    .build()));

            for (int i = 0; i < WORKER_COUNT; i++) {
                User emp = userRepository.save(User.builder()
                        .email("emp" + i + "-" + suffix + "@inout-test.com")
                        .password("encoded")
                        .name("직원" + i + "-" + suffix)
                        .phone(String.format("010-%04d-%04d", i / 100 + 1, i % 100))
                        .birthday(LocalDate.of(1990, 1, 1))
                        .store(store)
                        .build());
                employees.add(emp);

                OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                        .requestUser(emp)
                        .status(OrderStatus.REQUESTED)
                        .totalPrice(ITEM_PRICE)
                        .requestDate(LocalDateTime.now())
                        .receiverName("수령인" + i)
                        .receiverPhone(String.format("010-1234-%04d", i))
                        .destinationAddress("서울시 테스트구 " + i + "번지")
                        .build());

                orderDetailRepository.save(OrderDetail.builder()
                        .orderRequest(order)
                        .item(item)
                        .requestQuantity(1)
                        .itemPriceSnapshot(ITEM_PRICE)
                        .build());

                orders.add(order);
            }
        });
    }

    @AfterEach
    void tearDown() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(s -> {
            List<Long> accountIds = depositAccounts.stream()
                    .map(DepositAccount::getId)
                    .collect(Collectors.toList());
            List<DepositHistory> histories = new ArrayList<>();
            accountIds.forEach(id -> histories.addAll(
                    depositHistoryRepository
                            .findByDepositAccountIdOrderByCreatedAtDesc(id, Pageable.unpaged())
                            .getContent()));
            depositHistoryRepository.deleteAll(histories);
        });

        tx.executeWithoutResult(s -> stockUsageHistoryRepository.deleteAll(
                stockUsageHistoryRepository.findAllByItem_ItemId(item.getItemId())));

        tx.executeWithoutResult(s -> {
            Set<Long> orderIds = orders.stream()
                    .map(OrderRequest::getId)
                    .collect(Collectors.toSet());
            List<OrderDetail> details = orderDetailRepository.findAll().stream()
                    .filter(d -> orderIds.contains(d.getOrderRequest().getId()))
                    .collect(Collectors.toList());
            orderDetailRepository.deleteAll(details);
            orderRequestRepository.deleteAllById(new ArrayList<>(orderIds));
        });


        tx.executeWithoutResult(s -> {
            List<Long> accountIds = depositAccounts.stream()
                    .map(DepositAccount::getId)
                    .collect(Collectors.toList());
            depositAccountRepository.deleteAllById(accountIds);
        });

        tx.executeWithoutResult(s -> {
            itemRepository.deleteById(item.getItemId());
            itemCategoryRepository.deleteById(category.getCategoryId());
        });

        tx.executeWithoutResult(s -> {
            List<Long> userIds = new ArrayList<>(
                    employees.stream().map(User::getId).collect(Collectors.toList()));
            userIds.add(admin.getId());
            userRepository.deleteAllById(userIds);
            storeRepository.delete(store);
        });
    }

    @Test
    @DisplayName("[동시성] 100건 동시 결제: DepositHistory 100건 누락 없이 기록되고 balanceAfter가 정확하다")
    void concurrentPayment_100Payments_ledgerIsCompleteAndAccurate() throws Exception {

        AtomicInteger successCount = new AtomicInteger();
        runConcurrently(WORKER_COUNT, i -> {
            paymentService.processDepositPayment(
                    employees.get(i).getId(),
                    PaymentDto.Request.builder()
                            .orderId(orders.get(i).getId())
                            .amount(ITEM_PRICE)
                            .build());
            successCount.incrementAndGet();
        });

        assertThat(successCount.get())
                .as("100건 모두 결제 성공해야 한다")
                .isEqualTo(WORKER_COUNT);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

  
        tx.executeWithoutResult(s -> {
            List<OrderRequest> reloaded = orderRequestRepository.findAllById(
                    orders.stream().map(OrderRequest::getId).collect(Collectors.toList()));
            assertThat(reloaded)
                    .as("결제 후 모든 주문은 PAID 상태여야 한다")
                    .extracting(OrderRequest::getStatus)
                    .containsOnly(OrderStatus.PAID);
        });


        tx.executeWithoutResult(s -> {
            List<Long> accountIds = depositAccounts.stream()
                    .map(DepositAccount::getId)
                    .collect(Collectors.toList());

            List<DepositHistory> histories = new ArrayList<>();
            accountIds.forEach(id -> histories.addAll(
                    depositHistoryRepository
                            .findByDepositAccountIdOrderByCreatedAtDesc(id, Pageable.unpaged())
                            .getContent()));


            assertThat(histories)
                    .as("동시 결제 100건 후 DepositHistory는 정확히 %d건이어야 한다", WORKER_COUNT)
                    .hasSize(WORKER_COUNT);


            assertThat(histories)
                    .as("모든 이력의 거래 타입은 PAYMENT여야 한다")
                    .extracting(DepositHistory::getType)
                    .containsOnly(TransactionType.PAYMENT);


            assertThat(histories)
                    .as("모든 이력의 결제 금액은 %d원이어야 한다", ITEM_PRICE)
                    .extracting(DepositHistory::getAmount)
                    .containsOnly(ITEM_PRICE);
           
            long expectedFinalBalance = INITIAL_DEPOSIT * WORKER_COUNT - ITEM_PRICE * WORKER_COUNT;
            assertThat(histories)
                    .as("거래 후 잔액(balanceAfter)은 중복 없이 기록되어야 한다")
                    .extracting(DepositHistory::getBalanceAfter)
                    .doesNotHaveDuplicates();
            assertThat(histories.stream().mapToLong(DepositHistory::getBalanceAfter).min().orElse(-1))
                    .as("가장 낮은 balanceAfter는 최종 잔액과 같아야 한다")
                    .isEqualTo(expectedFinalBalance);
          
            assertThat(histories)
                    .as("결제 이력에는 연관 주문 ID가 반드시 기록되어야 한다")
                    .allSatisfy(h -> assertThat(h.getRelatedOrderId()).isNotNull());
        });

        tx.executeWithoutResult(s -> {
            long expectedBalance = INITIAL_DEPOSIT * WORKER_COUNT - ITEM_PRICE * WORKER_COUNT;
            depositAccounts.forEach(account -> {
                DepositAccount reloaded = depositAccountRepository
                        .findById(account.getId()).orElseThrow();
                assertThat(reloaded.getBalance())
                        .as("매장 계좌(id=%d) 최종 잔액은 %d원이어야 한다", account.getId(), expectedBalance)
                        .isEqualTo(expectedBalance);
            });
        });

        Item reloadedItem = itemRepository.findById(item.getItemId()).orElseThrow();
        assertThat(reloadedItem.getCurrentStock())
                .as("결제 단계에서는 재고가 변동되어서는 안 된다")
                .isEqualTo(WORKER_COUNT);
    }

    @Test
    @DisplayName("[동시성] 100건 동시 발주 승인: 재고 정확히 0 차감, StockUsageHistory 100건 기록, 원장 일관성 유지")
    void concurrentApproval_100Orders_stockAndLedgerConsistent() throws Exception {

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(s -> {
            List<Long> orderIds = orders.stream().map(OrderRequest::getId).collect(Collectors.toList());
            orderIds.forEach(orderId -> {
                OrderRequest order = orderRequestRepository.findByIdForUpdate(orderId).orElseThrow();
                order.updateStatus(OrderStatus.PAID);
            });
        });

        tx.executeWithoutResult(s -> {
            DepositAccount account = depositAccountRepository
                    .findByStoreIdForUpdate(store.getId()).orElseThrow();
            for (int i = 0; i < WORKER_COUNT; i++) {
                account.deductBalance(ITEM_PRICE);
                depositHistoryRepository.save(DepositHistory.builder()
                        .depositAccount(account)
                        .type(TransactionType.PAYMENT)
                        .amount(ITEM_PRICE)
                        .description("사전 결제 시뮬레이션 - 주문 " + orders.get(i).getId())
                        .processedBy(employees.get(i).getId())
                        .relatedOrderId(orders.get(i).getId())
                        .balanceAfter(account.getBalance())
                        .build());
            }
        });

        AtomicInteger approvedCount  = new AtomicInteger();
        AtomicInteger rejectedCount  = new AtomicInteger();
        runConcurrently(WORKER_COUNT, i -> {
            boolean approved = orderApprovalTxService
                    .processSingleOrderApproval(orders.get(i).getId(), admin.getId());
            if (approved) approvedCount.incrementAndGet();
            else          rejectedCount.incrementAndGet();
        });

        Item reloadedItem = itemRepository.findById(item.getItemId()).orElseThrow();
        assertThat(reloadedItem.getCurrentStock())
                .as("100건 동시 승인 후 재고는 정확히 0이어야 한다 (재고 초기값=%d)", WORKER_COUNT)
                .isZero();

        assertThat(approvedCount.get())
                .as("재고가 충분하므로 100건 모두 승인 처리되어야 한다")
                .isEqualTo(WORKER_COUNT);
        assertThat(rejectedCount.get())
                .as("재고 부족 자동 반려 건수는 0이어야 한다")
                .isZero();

        tx.executeWithoutResult(s -> {
            List<OrderRequest> reloaded = orderRequestRepository.findAllById(
                    orders.stream().map(OrderRequest::getId).collect(Collectors.toList()));
            assertThat(reloaded)
                    .as("승인 완료 후 모든 주문은 COMPLETED 상태여야 한다")
                    .extracting(OrderRequest::getStatus)
                    .containsOnly(OrderStatus.COMPLETED);
        });

        tx.executeWithoutResult(s -> {
            List<?> usages = stockUsageHistoryRepository.findAllByItem_ItemId(item.getItemId());
            assertThat(usages)
                    .as("재고 사용 이력은 승인 건수(100)와 일치해야 한다")
                    .hasSize(WORKER_COUNT);
        });

        tx.executeWithoutResult(s -> {
            List<Long> accountIds = depositAccounts.stream()
                    .map(DepositAccount::getId).collect(Collectors.toList());

            List<DepositHistory> allHistories = new ArrayList<>();
            accountIds.forEach(id -> allHistories.addAll(
                    depositHistoryRepository
                            .findByDepositAccountIdOrderByCreatedAtDesc(id, Pageable.unpaged())
                            .getContent()));


            assertThat(allHistories)
                    .as("전체 승인 처리 후 DepositHistory PAYMENT 레코드는 정확히 %d건이어야 한다", WORKER_COUNT)
                    .hasSize(WORKER_COUNT);

            long refundCount = allHistories.stream()
                    .filter(h -> h.getType() == TransactionType.REFUND).count();
            assertThat(refundCount)
                    .as("재고 부족 없이 전량 승인되었으므로 REFUND 기록은 0건이어야 한다")
                    .isZero();


            Map<Long, Long> balanceByAccountId = allHistories.stream()
                    .collect(Collectors.toMap(
                            h -> h.getDepositAccount().getId(),
                            DepositHistory::getBalanceAfter,
                            Math::min));

            balanceByAccountId.forEach((accountId, snapshotBalance) -> {
                DepositAccount account = depositAccountRepository.findById(accountId).orElseThrow();
                assertThat(snapshotBalance)
                        .as("계좌(id=%d)의 최종 balanceAfter 스냅샷은 실제 계좌 잔액과 일치해야 한다", accountId)
                        .isEqualTo(account.getBalance());
            });
        });
    }

    private void runConcurrently(int taskCount, IntConsumer task) throws Exception {
        int poolSize = Math.max(32, taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch readyLatch = new CountDownLatch(taskCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures   = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();      
                task.accept(index);
                return null;
            }));
        }

        try {
            assertThat(readyLatch.await(30, TimeUnit.SECONDS))
                    .as("모든 스레드가 30초 내에 준비 완료되어야 한다")
                    .isTrue();
            startLatch.countDown();         

            for (Future<?> future : futures) {
                future.get(120, TimeUnit.SECONDS);  
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                .as("ExecutorService가 10초 내에 종료되어야 한다")
                .isTrue();
    }
}
