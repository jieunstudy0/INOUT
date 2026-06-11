package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.order.service.CartEmpService;
import com.jstudy.inout.order.service.OrderApprovalTxService;
import com.jstudy.inout.order.service.OrderEmpService;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class StockConcurrencyTest {

    private static final int WORKER_COUNT = 100;

    @Autowired private CartEmpService cartEmpService;
    @Autowired private OrderEmpService orderEmpService;
    @Autowired private OrderApprovalTxService orderApprovalTxService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private CartDetailRepository cartDetailRepository;
    @Autowired private OrderRequestRepository orderRequestRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockBean private MailComponent mailComponent;
    @MockBean private DeliveryService deliveryService;

    @Test
    @DisplayName("동시 발주 승인 - 비관적 락으로 동일 상품 재고가 정확히 100개 차감된다")
    void concurrentOrderApproval_deductsStockWithoutLostUpdate() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Store store = storeRepository.save(Store.builder()
                .name("동시성매장-" + suffix)
                .address("서울시 종로구")
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin-" + suffix + "@inout.com")
                .password("encoded")
                .name("관리자")
                .phone("010-0000-0000")
                .birthday(LocalDate.of(1980, 1, 1))
                .store(store)
                .build());
        ItemCategory category = itemCategoryRepository.save(ItemCategory.builder()
                .categoryName("동시성카테고리-" + suffix)
                .build());
        Item item = itemRepository.save(Item.builder()
                .name("동시성상품-" + suffix)
                .category(category)
                .unitPrice(1000L)
                .currentStock(WORKER_COUNT)
                .minStockLevel(0)
                .deleted(false)
                .build());
        List<User> employees = createEmployees(store, suffix);
        Queue<Long> orderIds = new ConcurrentLinkedQueue<>();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        runConcurrently(WORKER_COUNT, index -> {
            User employee = employees.get(index);

            cartEmpService.addToCart(employee.getId(), new CartAddRequest(item.getItemId(), 1));
            List<Long> cartDetailIds = cartDetailRepository.findAllByCart_User_Id(employee.getId()).stream()
                    .filter(cartDetail -> !cartDetail.isDeleted())
                    .map(CartDetail::getCartDetailId)
                    .toList();

            orderEmpService.submitOrderRequest(employee.getId(), OrderCreateRequest.builder()
                    .cartDetailIds(cartDetailIds)
                    .build());

            Long orderId = orderRequestRepository
                    .findAllByRequestUser_IdOrderByRequestDateDesc(employee.getId())
                    .get(0)
                    .getId();
            tx.executeWithoutResult(status -> {
                OrderRequest order = orderRequestRepository.findByIdForUpdate(orderId).orElseThrow();
                order.updateStatus(OrderStatus.PAID);
            });
            orderIds.add(orderId);
        });

        assertThat(orderIds).hasSize(WORKER_COUNT);

        List<Long> approvalOrderIds = new ArrayList<>(orderIds);
        runConcurrently(WORKER_COUNT, index -> {
            boolean approved = orderApprovalTxService.processSingleOrderApproval(
                    approvalOrderIds.get(index),
                    admin.getId());
            assertThat(approved).isTrue();
        });

        Item reloadedItem = itemRepository.findById(item.getItemId()).orElseThrow();
        assertThat(reloadedItem.getCurrentStock()).isZero();
    }

    private List<User> createEmployees(Store store, String suffix) {
        List<User> employees = new ArrayList<>();
        for (int i = 0; i < WORKER_COUNT; i++) {
            employees.add(User.builder()
                    .email("employee-" + i + "-" + suffix + "@inout.com")
                    .password("encoded")
                    .name("직원" + i)
                    .phone(String.format("010-%04d-%04d", i, i))
                    .birthday(LocalDate.of(1990, 1, 1))
                    .store(store)
                    .build());
        }
        return userRepository.saveAll(employees);
    }

    private void runConcurrently(int taskCount, IntConsumer task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(32, taskCount));
        CountDownLatch readyLatch = new CountDownLatch(taskCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

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
            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
}
