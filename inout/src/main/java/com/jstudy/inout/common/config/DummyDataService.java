package com.jstudy.inout.common.config;

import com.jstudy.inout.common.auth.entity.*;
import com.jstudy.inout.common.auth.repository.*;
import com.jstudy.inout.delivery.entity.*;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.inquiry.repository.InquiryCommentRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.leave.repository.AnnualLeaveRepository;
import com.jstudy.inout.order.entity.*;
import com.jstudy.inout.order.repository.*;
import com.jstudy.inout.payment.entity.*;
import com.jstudy.inout.payment.repository.*;
import com.jstudy.inout.stock.entity.*;
import com.jstudy.inout.stock.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@Profile({"local", "demo", "secret"})
@RequiredArgsConstructor
public class DummyDataService {

    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ItemCategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final StockReceivingHistoryRepository receivingHistoryRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final DepositAccountRepository depositAccountRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final ChargeRequestRepository chargeRequestRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final DeliveryRepository deliveryRepository;
    private final CartDetailRepository cartDetailRepository;
    private final CartRepository cartRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final InquiryRepository inquiryRepository;

    @Transactional
    public void clearAllData() {
        log.info("기존 더미 데이터를 모두 삭제합니다.");
        deleteAllInFkSafeOrder();
    }

    /**
     * FK 의존성 순서: 자식 → intermediate → 부모(user/store).
     * charge_request.processor_id / annual_leave.processor_id 등이 user를 참조하므로
     * user 삭제 전에 반드시 제거한다.
     */
    private void deleteAllInFkSafeOrder() {
        // 1) 최하위 자식
        inquiryCommentRepository.deleteAll();
        inquiryCommentRepository.flush();
        inquiryRepository.deleteAllInBatch();

        cartDetailRepository.deleteAllInBatch();
        cartRepository.deleteAllInBatch();

        deliveryRepository.deleteAllInBatch();
        orderDetailRepository.deleteAllInBatch();
        orderRequestRepository.deleteAllInBatch();

        annualLeaveRepository.deleteAllInBatch();
        chargeRequestRepository.deleteAllInBatch();
        depositHistoryRepository.deleteAllInBatch();
        depositAccountRepository.deleteAllInBatch();

        usageHistoryRepository.deleteAllInBatch();
        receivingHistoryRepository.deleteAllInBatch();

        // 2) intermediate
        itemRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        safeClearRefreshTokens();
        userRoleRepository.deleteAllInBatch();

        // 3) 최상위 부모
        userRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
    }

    private void safeClearRefreshTokens() {
        try {
            refreshTokenRepository.deleteAllInBatch();
        } catch (Exception e) {
            log.warn("RefreshToken 삭제 스킵 (Redis/쿠키 세션 관리 방식 사용 중): {}", e.getMessage());
        }
    }

    @Transactional
    public void resetUsersOnly() {
        log.info("사용자 관련 더미 데이터를 초기화합니다.");
        deleteAllInFkSafeOrder();
        generateDummyData();
    }

    @Transactional
    public void generateDummyData() {
        log.info("데모용 더미 데이터를 새롭게 생성합니다...");
        String defaultPw = passwordEncoder.encode("inout1234!");

        Role adminRole = roleRepository.save(Role.builder().roleName("ROLE_ADMIN").build());
        Role ownerRole = roleRepository.save(Role.builder().roleName("ROLE_OWNER").build());
        Role empRole = roleRepository.save(Role.builder().roleName("ROLE_EMPLOYEE").build());

        Store hq = storeRepository.save(Store.builder().name("본사").address("서울 강남구").phone("02-000-0000").build());
        List<Store> branches = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            branches.add(storeRepository.save(Store.builder().name("지점 " + i + "호").address("서울 마포구 " + i + "길").phone("02-111-100" + i).build()));
        }

        User admin1 = createUser("admin1@test.com", "김본사", null, defaultPw, adminRole);
        User admin2 = createUser("admin2@test.com", "이본사", null, defaultPw, adminRole);

        
        for (int i = 0; i < branches.size(); i++) {
            Store branch = branches.get(i);
            createUser("owner" + (i + 1) + "@test.com", "점주" + (i + 1), branch, defaultPw, ownerRole);
            createDepositAccount(branch, 50_000_000L, admin1); 
        }

      
        List<User> employees = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            UserStatus status = (i == 20) ? UserStatus.ON_LEAVE : UserStatus.ACTIVE;
            Store assignedStore = branches.get(i % 5);
            User emp = createUser("emp" + i + "@test.com", "직원" + i, assignedStore, defaultPw, empRole);
            emp.updateStatusAndStore(status, assignedStore);
            // emp19: 로그인 실패 잠금 시연용
            if (i == 19) {
                for (int f = 0; f < 5; f++) {
                    emp.increaseFailedAttempt();
                }
            }
            employees.add(emp);
        }

   
        ItemCategory catCoffee = categoryRepository.save(ItemCategory.builder().categoryName("커피/원두").build());
        ItemCategory catSupply = categoryRepository.save(ItemCategory.builder().categoryName("소모품/포장재").build());

        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            items.add(createItem("원두 블렌드 Type-" + i, catCoffee, 15000L + (i * 1000), 10, 100, admin1));
        }
        for (int i = 1; i <= 3; i++) {
            items.add(createItem("테이크아웃 컵 " + i + "oz", catSupply, 45000L, 10, 5, admin1));
        }
        items.add(createItem("바닐라 시럽 1L", catSupply, 12000L, 10, 0, admin1));
        items.add(createItem("헤이즐넛 시럽 1L", catSupply, 12000L, 10, 0, admin1));

   
        Random random = new Random();
        for (int i = 0; i < employees.size(); i++) {
            User emp = employees.get(i);
            Item orderItem = items.get(random.nextInt(15)); 

            int mod = i % 5;
            if (mod == 0) {
                createOrder(emp, orderItem, 5, OrderStatus.REQUESTED, OrderDetailStatus.WAITING, null);
            } else if (mod == 1) {
                OrderRequest order = createOrder(emp, orderItem, 2, OrderStatus.ORDERED, OrderDetailStatus.WAITING, null);
                deductDeposit(emp, order.getTotalPrice(), order.getId());
            } else if (mod == 2) {
                OrderRequest order = createOrder(emp, orderItem, 3, OrderStatus.APPROVED, OrderDetailStatus.APPROVED, null);
                deductDeposit(emp, order.getTotalPrice(), order.getId());
                deductStock(orderItem, 3, admin1, order.getId());
                createDelivery(order, DeliveryStatus.READY);
            } else if (mod == 3) {
                OrderRequest order = createOrder(emp, orderItem, 10, OrderStatus.REJECTED, OrderDetailStatus.REJECTED, "재고 부족으로 인한 반려");
                deductDeposit(emp, order.getTotalPrice(), order.getId());
                refundDeposit(emp, order.getTotalPrice(), order.getId(), admin1);
            } else if (mod == 4) {
                OrderRequest order = createOrder(emp, orderItem, 5, OrderStatus.APPROVED, OrderDetailStatus.APPROVED, null);
                deductDeposit(emp, order.getTotalPrice(), order.getId());
                deductStock(orderItem, 5, admin1, order.getId());
                createDelivery(order, DeliveryStatus.COMPLETED);
            }
        }
        log.info("더미 데이터 세팅이 완료되었습니다! (3역할: ADMIN / OWNER / EMPLOYEE)");
    }

    private User createUser(String email, String name, Store store, String pw, Role role) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(pw)
                .name(name)
                .phone("010-0000-0000")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .isLocked(false)
                .loginFailCount(0)
                .build());
        userRoleRepository.save(UserRole.builder().user(user).role(role).build());
        return user;
    }

  
    private void createDepositAccount(Store store, Long amount, User admin) {
        if (store == null) {
            log.warn("store가 null이므로 DepositAccount를 생성하지 않습니다.");
            return;
        }

        DepositAccount account = depositAccountRepository.save(
                DepositAccount.builder()
                        .store(store) 
                        .balance(amount)
                        .build()
        );

        depositHistoryRepository.save(DepositHistory.builder()
                .depositAccount(account)
                .type(TransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .description("테스트 매장 초기 지원금")
                .processedBy(admin != null ? admin.getId() : null)
                .build());
    }

    private Item createItem(String name, ItemCategory cat, Long price, int minStock, int initialStock, User admin) {
        Item item = itemRepository.save(Item.builder().name(name).category(cat).unitPrice(price)
                .minStockLevel(minStock).currentStock(initialStock).unitDescription("EA").deleted(false).build());
        if (initialStock > 0) {
            receivingHistoryRepository.save(StockReceivingHistory.builder().item(item).user(admin)
                    .receivingQuantity(initialStock).resultStock(initialStock).memo("초기 입고").build());
        }
        return item;
    }

    private OrderRequest createOrder(User emp, Item item, int qty, OrderStatus status, OrderDetailStatus detailStatus, String rejectReason) {
        long totalPrice = item.getUnitPrice() * qty;
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(emp).status(status).totalPrice(totalPrice).requestDate(LocalDateTime.now().minusDays(1))
                .processDate(status != OrderStatus.REQUESTED ? LocalDateTime.now() : null)
                .receiverName(emp.getName()).receiverPhone(emp.getPhone()).destinationAddress(emp.getStore() != null ? emp.getStore().getAddress() : "서울 강남구")
                .rejectReason(rejectReason).build());

        orderDetailRepository.save(OrderDetail.builder().orderRequest(order).item(item).requestQuantity(qty)
                .itemPriceSnapshot(item.getUnitPrice()).status(detailStatus).build());
        return order;
    }

    private void deductDeposit(User emp, Long amount, Long orderId) {
        if (emp.getStore() == null) return;
        DepositAccount acc = depositAccountRepository.findByStoreIdForUpdate(emp.getStore().getId()).orElseThrow();
        acc.deductBalance(amount);
        depositHistoryRepository.save(DepositHistory.builder().depositAccount(acc).type(TransactionType.PAYMENT)
                .amount(amount).description("주문 결제").relatedOrderId(orderId).processedBy(emp.getId())
                .balanceAfter(acc.getBalance()).build());
    }

    private void refundDeposit(User emp, Long amount, Long orderId, User admin) {
        if (emp.getStore() == null) return;
        DepositAccount acc = depositAccountRepository.findByStoreIdForUpdate(emp.getStore().getId()).orElseThrow();
        acc.addBalance(amount);
        depositHistoryRepository.save(DepositHistory.builder().depositAccount(acc).type(TransactionType.REFUND)
                .amount(amount).description("주문 반려 환불").relatedOrderId(orderId).processedBy(admin != null ? admin.getId() : null)
                .balanceAfter(acc.getBalance()).build());
    }

    private void deductStock(Item item, int qty, User admin, Long orderId) {
        item.removeStock(qty);
        usageHistoryRepository.save(StockUsageHistory.builder().item(item).user(admin)
                .usageQuantity(qty).resultStock(item.getCurrentStock()).memo("발주 승인 (#" + orderId + ")").build());
    }

    private void createDelivery(OrderRequest order, DeliveryStatus status) {
        boolean hasWaybill = status != DeliveryStatus.READY;
        deliveryRepository.save(Delivery.builder().orderRequest(order).status(status)
                .receiverName(order.getReceiverName()).receiverPhone(order.getReceiverPhone())
                .destinationAddress(order.getDestinationAddress())
                .carrier(hasWaybill ? DeliveryService.CARRIER_CJ : null)
                .trackingNumber(hasWaybill ? generateCjStyleTrackingNumber() : null)
                .shippedAt(hasWaybill ? LocalDateTime.now().minusDays(2) : null)
                .deliveredAt(status == DeliveryStatus.COMPLETED ? LocalDateTime.now().minusHours(6) : null)
                .build());
    }

    /** CJ대한통운 Mock 송장: 56 + 10자리 난수 */
    private String generateCjStyleTrackingNumber() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder("56");
        for (int i = 0; i < 10; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }
}