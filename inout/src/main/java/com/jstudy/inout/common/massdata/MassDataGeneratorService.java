package com.jstudy.inout.common.massdata;

import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.massdata.dto.MassDataGenerationResponse;
import com.jstudy.inout.common.massdata.dto.MassDataStepResult;
import com.jstudy.inout.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class MassDataGeneratorService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final DashboardService dashboardService;

    private static final Faker faker = new Faker();

    private static final int MIN_SCALE = 1;
    private static final int MAX_SCALE = 20; 
    private static final int CHUNK_SIZE = 500; 

    private static final int BASE_MEMBER = 100;   
    private static final int BASE_CATEGORY = 100; 
    private static final int BASE_PRODUCT = 100; 
    private static final int BASE_TXN = 300;      

    private static final int FIXED_ADMIN_COUNT = 2; 
    private static final int HOT_ITEM_COUNT = 3;      
    private static final long INITIAL_DEPOSIT_BALANCE = 50_000_000L;

    private static final String[] SURNAMES =
            {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "권", "황"};
    private static final String[] CITIES =
            {"서울", "부산", "대구", "인천", "광주", "대전", "울산", "수원", "성남", "고양", "용인", "청주", "전주", "포항", "창원"};
    private static final String[] UNIT_DESCRIPTIONS = {"EA", "BOX", "SET", "PACK", "KG"};
    private static final String[] INQUIRY_TOPICS = {"배송 지연", "교환 및 환불", "상품 품질", "결제 오류", "기타 문의"};
    private static final String[] LEAVE_TYPES = {"ANNUAL", "HALF_DAY", "SICK"};
    private static final String[] LEAVE_REASONS = {"개인 사유", "가족 여행", "병원 진료", "경조사", "휴식"};


    private record EmployeeInfo(long userId, String name, String phone, long storeId) {}

    private record UserGenResult(List<Long> adminIds, List<EmployeeInfo> employees) {}

    private record ItemGenResult(List<Long> allItemIds, List<Long> hotItemIds,
                                  Map<Long, Integer> initialStock, Map<Long, Long> unitPriceById) {}

    private record DepositAccountResult(Map<Long, Long> accountIdByStoreId, Map<Long, long[]> balanceByStoreId) {}

    private record InquiryGenResult(List<Long> inquiryIds, List<Boolean> waitingFlags) {}

    private record OrderPlan(long employeeUserId, String employeeName, String employeePhone, long storeId,
                              long itemId, long unitPrice, int qty,
                              String status, String detailStatus, String rejectReason,
                              LocalDateTime requestDate, LocalDateTime processDate) {}


    @Transactional
    public MassDataGenerationResponse generate(int scale) {
        if (scale < MIN_SCALE || scale > MAX_SCALE) {
            throw new InoutException(
                    "scale은 " + MIN_SCALE + " 이상 " + MAX_SCALE + " 이하만 허용됩니다 (서버 리소스 보호를 위한 상한). 입력값: " + scale,
                    400, "INVALID_SCALE");
        }

        long totalStart = System.currentTimeMillis();
        String runTag = Long.toString(System.nanoTime() % 100_000_000L); 
        List<MassDataStepResult> steps = new ArrayList<>();

        int memberCount = BASE_MEMBER * scale;
        int categoryCount = BASE_CATEGORY * scale;
        int productCount = BASE_PRODUCT * scale;
        int storeCount = Math.max(5, 10 * scale); 
        int orderCount = BASE_TXN * scale;
        int inquiryCount = BASE_TXN * scale;
        int annualLeaveCount = 50 * scale;
        int chargeRequestCount = 50 * scale;

        log.info("========== [대량 더미 데이터 생성 시작] scale={}, runTag={} (회원 {}, 카테고리 {}, 상품 {}, 주문/문의 {}) ==========",
                scale, runTag, memberCount, categoryCount, productCount, orderCount);

        long t = System.currentTimeMillis();
        Map<String, Long> roleIds = ensureRoles();
        recordStep(steps, "Role(lookup)", roleIds.size(), t);

        t = System.currentTimeMillis();
        Map<Long, String> storeAddressById = new LinkedHashMap<>();
        List<Long> storeIds = insertStores(storeCount, runTag, storeAddressById);
        recordStep(steps, "Store", storeIds.size(), t);

        t = System.currentTimeMillis();
        UserGenResult userResult = insertUsers(memberCount, storeIds, roleIds, runTag);
        recordStep(steps, "User(Member) + UserRole", memberCount, t);
        long adminId = userResult.adminIds().get(0);
        List<EmployeeInfo> employees = userResult.employees();

        t = System.currentTimeMillis();
        List<Long> categoryIds = insertCategories(categoryCount, runTag);
        recordStep(steps, "ItemCategory", categoryIds.size(), t);

        t = System.currentTimeMillis();
        ItemGenResult itemResult = insertItems(productCount, categoryIds, runTag);
        recordStep(steps, "Item(Product)", itemResult.allItemIds().size(), t);

        t = System.currentTimeMillis();
        List<Long> receivingIds = insertInitialStockReceiving(itemResult, adminId);
        recordStep(steps, "StockReceivingHistory", receivingIds.size(), t);

        t = System.currentTimeMillis();
        DepositAccountResult depositAccounts = insertDepositAccounts(storeIds, INITIAL_DEPOSIT_BALANCE);
        recordStep(steps, "DepositAccount", depositAccounts.accountIdByStoreId().size(), t);

        t = System.currentTimeMillis();
        List<Long> initialChargeIds = insertInitialDepositCharge(depositAccounts, adminId);
        recordStep(steps, "DepositHistory(초기 충전)", initialChargeIds.size(), t);

        List<OrderPlan> orderPlans = buildOrderPlans(orderCount, employees, itemResult);

        t = System.currentTimeMillis();
        List<Long> orderIds = insertOrderRequests(orderPlans, adminId, storeAddressById);
        recordStep(steps, "OrderRequest", orderIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> orderDetailIds = insertOrderDetails(orderPlans, orderIds);
        recordStep(steps, "OrderDetail", orderDetailIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> deliveryIds = insertDeliveries(orderPlans, orderIds, storeAddressById);
        recordStep(steps, "Delivery", deliveryIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> orderDepositIds = insertOrderDrivenDepositHistory(orderPlans, orderIds, depositAccounts, adminId);
        recordStep(steps, "DepositHistory(결제/환불)", orderDepositIds.size(), t);
       
        t = System.currentTimeMillis();
        Map<Long, Integer> stockLedger = new HashMap<>(itemResult.initialStock());
        List<Object[]> usageArgs = new ArrayList<>();
        appendHotItemUsageBurst(itemResult.hotItemIds(), stockLedger, adminId, usageArgs);
        appendOrderDrivenStockUsage(orderPlans, stockLedger, adminId, usageArgs);
        List<Long> usageIds = insertAndGetIds("stock_usage_history",
                "INSERT INTO `stock_usage_history` (`item_id`,`user_id`,`memo`,`result_stock`,`usage_date`,`usage_quantity`) VALUES (?,?,?,?,?,?)",
                usageArgs);
        recordStep(steps, "StockUsageHistory(인기상품 급증 포함)", usageIds.size(), t);

        t = System.currentTimeMillis();
        reconcileItemStock(stockLedger);
        recordStep(steps, "Item.currentStock 정합화", stockLedger.size(), t);

        t = System.currentTimeMillis();
        InquiryGenResult inquiryResult = insertInquiries(inquiryCount, employees, runTag);
        recordStep(steps, "Inquiry", inquiryResult.inquiryIds().size(), t);

        t = System.currentTimeMillis();
        List<Long> commentIds = insertInquiryComments(inquiryResult, adminId);
        recordStep(steps, "InquiryComment", commentIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> cartIds = insertCarts(employees);
        recordStep(steps, "Cart", cartIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> cartDetailIds = insertCartDetails(cartIds, itemResult.allItemIds());
        recordStep(steps, "CartDetail", cartDetailIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> leaveIds = insertAnnualLeaves(annualLeaveCount, employees, adminId);
        recordStep(steps, "AnnualLeave", leaveIds.size(), t);

        t = System.currentTimeMillis();
        List<Long> chargeIds = insertChargeRequests(chargeRequestCount, employees, adminId);
        recordStep(steps, "ChargeRequest", chargeIds.size(), t);

        dashboardService.evictDashboardSummary();

        long totalElapsed = System.currentTimeMillis() - totalStart;
        MassDataGenerationResponse response = MassDataGenerationResponse.of(scale, totalElapsed, steps);
        log.info("========== [대량 더미 데이터 생성 완료] scale={}, 총 {}건, {}ms ==========",
                scale, response.totalInserted(), totalElapsed);
        return response;
    }


    private void recordStep(List<MassDataStepResult> steps, String name, int count, long startMs) {
        long elapsed = System.currentTimeMillis() - startMs;
        steps.add(new MassDataStepResult(name, count, elapsed));
        log.info("[대량 더미 데이터] {} - {}건 삽입 완료 ({}ms)", name, count, elapsed);
    }

    private long getNextAutoIncrementId(String tableName) {
        Long next = jdbcTemplate.queryForObject(
                "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Long.class, tableName);
        if (next == null) {
            throw new InoutException("테이블의 다음 AUTO_INCREMENT 값을 조회할 수 없습니다: " + tableName,
                    500, "MASS_DATA_SCHEMA_ERROR");
        }
        return next;
    }

    private void chunkedBatchUpdate(String sql, List<Object[]> args) {
        for (int i = 0; i < args.size(); i += CHUNK_SIZE) {
            jdbcTemplate.batchUpdate(sql, args.subList(i, Math.min(i + CHUNK_SIZE, args.size())));
        }
    }

    private List<Long> insertAndGetIds(String tableName, String sql, List<Object[]> args) {
        if (args.isEmpty()) {
            return List.of();
        }
        long startId = getNextAutoIncrementId(tableName);
        chunkedBatchUpdate(sql, args);
        List<Long> ids = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            ids.add(startId + i);
        }
        return ids;
    }

    private static <T> T pick(T[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)];
    }


    private Map<String, Long> ensureRoles() {
        Map<String, Long> ids = new HashMap<>();
        for (String roleName : List.of("ROLE_ADMIN", "ROLE_OWNER", "ROLE_EMPLOYEE")) {
            List<Long> existing = jdbcTemplate.query(
                    "SELECT role_id FROM `role` WHERE role_name = ?",
                    (rs, rowNum) -> rs.getLong(1), roleName);
            long id;
            if (existing.isEmpty()) {
                List<Object[]> insertArgs = new ArrayList<>();
                insertArgs.add(new Object[]{roleName});
                id = insertAndGetIds("role", "INSERT INTO `role` (`role_name`) VALUES (?)", insertArgs).get(0);
            } else {
                id = existing.get(0);
            }
            ids.put(roleName, id);
        }
        return ids;
    }


    private List<Long> insertStores(int count, String runTag, Map<Long, String> addressByIdSink) {
        List<Object[]> args = new ArrayList<>(count);
        List<String> addresses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String address = pick(CITIES) + " " + (1 + ThreadLocalRandom.current().nextInt(20)) + "길 "
                    + (1 + ThreadLocalRandom.current().nextInt(100));
            addresses.add(address);
            args.add(new Object[]{
                    "지점 " + runTag + "-" + i + "호",
                    address,
                    "02-" + (1000 + ThreadLocalRandom.current().nextInt(9000)) + "-"
                            + (1000 + ThreadLocalRandom.current().nextInt(9000))
            });
        }
        List<Long> ids = insertAndGetIds("store",
                "INSERT INTO `store` (`name`,`address`,`phone`) VALUES (?,?,?)", args);
        for (int i = 0; i < ids.size(); i++) {
            addressByIdSink.put(ids.get(i), addresses.get(i));
        }
        return ids;
    }


    private UserGenResult insertUsers(int memberCount, List<Long> storeIds, Map<String, Long> roleIds, String runTag) {

        String encodedPw = passwordEncoder.encode("inout1234!");

        List<Object[]> args = new ArrayList<>(memberCount);
        List<String> names = new ArrayList<>(memberCount);
        List<String> phones = new ArrayList<>(memberCount);
        List<Long> assignedStoreIds = new ArrayList<>(memberCount);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < memberCount; i++) {
            boolean isAdmin = i < FIXED_ADMIN_COUNT;
            String name = pick(SURNAMES) + (isAdmin ? "관리자" : "직원") + i;
            String phone = "010-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000))
                    + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            long storeId = storeIds.get(i % storeIds.size());
            String email = (isAdmin ? "admin" : "emp") + runTag + "_" + i + "@inout-demo.com";
            LocalDate birthday = LocalDate.of(1985 + ThreadLocalRandom.current().nextInt(20),
                    1 + ThreadLocalRandom.current().nextInt(12), 1 + ThreadLocalRandom.current().nextInt(28));

            names.add(name);
            phones.add(phone);
            assignedStoreIds.add(storeId);
            args.add(new Object[]{
                    email, encodedPw, name, phone, storeId, birthday, "ACTIVE",
                    false, false, 0, false, now, now
            });
        }

        List<Long> userIds = insertAndGetIds("user",
                "INSERT INTO `user` (`email`,`password`,`name`,`phone`,`store_id`,`birthday`,`status`," +
                        "`password_reset_yn`,`deleted`,`login_fail_count`,`is_locked`,`created_at`,`updated_at`) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                args);

        long adminRoleId = roleIds.get("ROLE_ADMIN");
        long ownerRoleId = roleIds.get("ROLE_OWNER");
        long employeeRoleId = roleIds.get("ROLE_EMPLOYEE");
        List<Object[]> userRoleArgs = new ArrayList<>(memberCount);
        List<Long> adminIds = new ArrayList<>();
        List<EmployeeInfo> employees = new ArrayList<>();
        java.util.Set<Long> storesWithOwner = new java.util.HashSet<>();
        for (int i = 0; i < userIds.size(); i++) {
            long userId = userIds.get(i);
            boolean isAdmin = i < FIXED_ADMIN_COUNT;
            if (isAdmin) {
                userRoleArgs.add(new Object[]{userId, adminRoleId});
                adminIds.add(userId);
            } else {
                long storeId = assignedStoreIds.get(i);
                boolean makeOwner = storesWithOwner.add(storeId);
                userRoleArgs.add(new Object[]{userId, makeOwner ? ownerRoleId : employeeRoleId});
                employees.add(new EmployeeInfo(userId, names.get(i), phones.get(i), storeId));
            }
        }
        jdbcTemplate.batchUpdate("INSERT INTO `user_role` (`user_id`,`role_id`) VALUES (?,?)", userRoleArgs);

        return new UserGenResult(adminIds, employees);
    }


    private List<Long> insertCategories(int count, String runTag) {
        String[] words = {"커피/원두", "제과제빵", "소모품", "포장재", "비품", "음료", "베이커리", "유제품", "시럽/소스", "텀블러/용기"};
        List<Object[]> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            args.add(new Object[]{words[i % words.length] + " " + runTag + "-" + i});
        }
        return insertAndGetIds("item_category", "INSERT INTO `item_category` (`name`) VALUES (?)", args);
    }

    private ItemGenResult insertItems(int productCount, List<Long> categoryIds, String runTag) {
        List<Object[]> args = new ArrayList<>(productCount);
        List<Integer> stocks = new ArrayList<>(productCount);
        List<Long> prices = new ArrayList<>(productCount);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < productCount; i++) {
            boolean hot = i < HOT_ITEM_COUNT;
            int minStock = 10 + ThreadLocalRandom.current().nextInt(20);
            int currentStock = hot
                    ? 3 + ThreadLocalRandom.current().nextInt(8)
                    : minStock + 20 + ThreadLocalRandom.current().nextInt(300);
            long unitPrice = 3000 + ThreadLocalRandom.current().nextInt(48) * 1000L;
            String name = faker.commerce().productName() + " #" + runTag + "-" + i;
            String description = faker.lorem().sentence(12);

            stocks.add(currentStock);
            prices.add(unitPrice);
            args.add(new Object[]{
                    0L, categoryIds.get(i % categoryIds.size()), name, unitPrice,
                    currentStock, minStock, pick(UNIT_DESCRIPTIONS), description, false, now, now
            });
        }

        List<Long> itemIds = insertAndGetIds("item",
                "INSERT INTO `item` (`version`,`category_id`,`name`,`unit_price`,`current_stock`,`min_stock_level`," +
                        "`unit_description`,`description`,`deleted`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                args);

        Map<Long, Integer> initialStock = new LinkedHashMap<>();
        Map<Long, Long> unitPriceById = new LinkedHashMap<>();
        List<Long> hotItemIds = new ArrayList<>();
        for (int i = 0; i < itemIds.size(); i++) {
            long id = itemIds.get(i);
            initialStock.put(id, stocks.get(i));
            unitPriceById.put(id, prices.get(i));
            if (i < HOT_ITEM_COUNT) {
                hotItemIds.add(id);
            }
        }
        return new ItemGenResult(itemIds, hotItemIds, initialStock, unitPriceById);
    }


    private List<Long> insertInitialStockReceiving(ItemGenResult itemResult, long adminUserId) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().minusDays(90);
        for (Map.Entry<Long, Integer> e : itemResult.initialStock().entrySet()) {
            args.add(new Object[]{e.getKey(), adminUserId, "초기 입고 (더미 데이터)", e.getValue(), now, e.getValue()});
        }
        return insertAndGetIds("stock_receiving_history",
                "INSERT INTO `stock_receiving_history` (`item_id`,`user_id`,`memo`,`result_stock`,`process_date`,`receiving_quantity`) " +
                        "VALUES (?,?,?,?,?,?)",
                args);
    }


    private DepositAccountResult insertDepositAccounts(List<Long> storeIds, long initialBalance) {
        List<Object[]> args = new ArrayList<>(storeIds.size());
        for (Long storeId : storeIds) {
            args.add(new Object[]{storeId, initialBalance, 0L});
        }
        List<Long> ids = insertAndGetIds("deposit_account",
                "INSERT INTO `deposit_account` (`store_id`,`balance`,`version`) VALUES (?,?,?)", args);

        Map<Long, Long> accountIdByStoreId = new HashMap<>();
        Map<Long, long[]> balanceByStoreId = new HashMap<>();
        for (int i = 0; i < storeIds.size(); i++) {
            accountIdByStoreId.put(storeIds.get(i), ids.get(i));
            balanceByStoreId.put(storeIds.get(i), new long[]{initialBalance});
        }
        return new DepositAccountResult(accountIdByStoreId, balanceByStoreId);
    }

    private List<Long> insertInitialDepositCharge(DepositAccountResult accounts, long adminUserId) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime chargedAt = LocalDateTime.now().minusDays(90);
        for (Map.Entry<Long, Long> e : accounts.accountIdByStoreId().entrySet()) {
            long balance = accounts.balanceByStoreId().get(e.getKey())[0];
            args.add(new Object[]{e.getValue(), "CHARGE", balance, "테스트 계정 초기 지원금", null, adminUserId, balance, null,
                    chargedAt, chargedAt});
        }
        return insertAndGetIds("deposit_history",
                "INSERT INTO `deposit_history` (`deposit_account_id`,`type`,`amount`,`description`,`related_order_id`," +
                        "`processed_by`,`balance_after`,`admin_memo`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?)",
                args);
    }


    private List<OrderPlan> buildOrderPlans(int orderCount, List<EmployeeInfo> employees, ItemGenResult itemResult) {
        List<OrderPlan> plans = new ArrayList<>(orderCount);
        List<Long> allItemIds = itemResult.allItemIds();
        List<Long> hotItemIds = itemResult.hotItemIds();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < orderCount; i++) {
            EmployeeInfo emp = employees.get(i % employees.size());          
            boolean useHotItem = !hotItemIds.isEmpty() && (i % 4 == 0);
            long itemId = useHotItem
                    ? hotItemIds.get(i % hotItemIds.size())
                    : allItemIds.get(ThreadLocalRandom.current().nextInt(allItemIds.size()));
            long unitPrice = itemResult.unitPriceById().get(itemId);
            int qty = 1 + ThreadLocalRandom.current().nextInt(5);

            LocalDateTime requestDate = (ThreadLocalRandom.current().nextInt(100) < 30)
                    ? now.minusDays(ThreadLocalRandom.current().nextInt(7)).minusHours(ThreadLocalRandom.current().nextInt(24))
                    : now.minusDays(ThreadLocalRandom.current().nextInt(45));

            int scenario = i % 5;
            String status;
            String detailStatus;
            String rejectReason = null;
            LocalDateTime processDate = null;
            switch (scenario) {
                case 0 -> {
                    status = "REQUESTED";
                    detailStatus = "WAITING";
                }
                case 1 -> {
                    status = "PAID";
                    detailStatus = "WAITING";
                    processDate = requestDate.plusHours(1);
                }
                case 2 -> {
                    status = "COMPLETED";
                    detailStatus = "APPROVED";
                    processDate = requestDate.plusHours(2);
                }
                case 3 -> {
                    status = "REJECTED";
                    detailStatus = "REJECTED";
                    processDate = requestDate.plusHours(1);
                    rejectReason = "재고 부족으로 인한 발주 반려";
                }
                default -> {
                    status = "COMPLETED";
                    detailStatus = "APPROVED";
                    processDate = requestDate.plusHours(3);
                }
            }

            plans.add(new OrderPlan(emp.userId(), emp.name(), emp.phone(), emp.storeId(),
                    itemId, unitPrice, qty, status, detailStatus, rejectReason, requestDate, processDate));
        }
        return plans;
    }

    private List<Long> insertOrderRequests(List<OrderPlan> plans, long adminUserId, Map<Long, String> storeAddressById) {
        List<Object[]> args = new ArrayList<>(plans.size());
        for (OrderPlan p : plans) {
            long totalPrice = p.unitPrice() * p.qty();
            Long processUserId = "REQUESTED".equals(p.status()) ? null : adminUserId;
            args.add(new Object[]{
                    p.employeeUserId(), processUserId, p.status(), totalPrice,
                    p.requestDate(), p.processDate(), p.rejectReason(),
                    p.employeeName(), p.employeePhone(), storeAddressById.getOrDefault(p.storeId(), "주소 미상"),
                    "대량 더미 데이터 생성"
            });
        }
        return insertAndGetIds("order_request",
                "INSERT INTO `order_request` (`request_user_id`,`process_user_id`,`status`,`total_price`,`request_date`," +
                        "`process_date`,`reject_reason`,`receiver_name`,`receiver_phone`,`destination_address`,`memo`) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                args);
    }

    private List<Long> insertOrderDetails(List<OrderPlan> plans, List<Long> orderIds) {
        List<Object[]> args = new ArrayList<>(plans.size());
        for (int i = 0; i < plans.size(); i++) {
            OrderPlan p = plans.get(i);
            args.add(new Object[]{p.itemId(), p.qty(), p.unitPrice(), p.requestDate(), p.detailStatus(),
                    orderIds.get(i), false, null});
        }
        return insertAndGetIds("order_detail",
                "INSERT INTO `order_detail` (`item_id`,`request_quantity`,`item_price_snapshot`,`created_at`,`status`," +
                        "`order_id`,`is_ai_suggested`,`ai_reason`) VALUES (?,?,?,?,?,?,?,?)",
                args);
    }

    private List<Long> insertDeliveries(List<OrderPlan> plans, List<Long> orderIds, Map<Long, String> storeAddressById) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < plans.size(); i++) {
            OrderPlan p = plans.get(i);
            if (!"COMPLETED".equals(p.status())) {
                continue; 
            }
            boolean delivered = ThreadLocalRandom.current().nextBoolean();
            String status = delivered ? "COMPLETED" : "SHIPPING";
            LocalDateTime shippedAt = p.processDate().plusHours(1);
            LocalDateTime deliveredAt = delivered ? shippedAt.plusDays(1) : null;

            args.add(new Object[]{
                    0L, orderIds.get(i), status, p.employeeName(), p.employeePhone(),
                    storeAddressById.getOrDefault(p.storeId(), "주소 미상"),
                    "CJ" + (System.nanoTime() % 1_000_000_000L) + "-" + i,
                    shippedAt, deliveredAt, LocalDateTime.now(), LocalDateTime.now()
            });
        }
        return insertAndGetIds("delivery",
                "INSERT INTO `delivery` (`version`,`order_id`,`status`,`receiver_name`,`receiver_phone`,`destination_address`," +
                        "`tracking_number`,`shipped_at`,`delivered_at`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                args);
    }

    private List<Long> insertOrderDrivenDepositHistory(List<OrderPlan> plans, List<Long> orderIds,
                                                         DepositAccountResult accounts, long adminUserId) {
        List<Object[]> args = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            OrderPlan p = plans.get(i);
            if ("REQUESTED".equals(p.status())) {
                continue;
            }
            Long accountId = accounts.accountIdByStoreId().get(p.storeId());
            long[] balanceHolder = accounts.balanceByStoreId().get(p.storeId());
            if (accountId == null || balanceHolder == null) {
                continue;
            }
            long amount = p.unitPrice() * p.qty();
            long orderId = orderIds.get(i);
            LocalDateTime paymentTime = p.requestDate().plusMinutes(10);

            long balanceAfterPayment = Math.max(0, balanceHolder[0] - amount);
            balanceHolder[0] = balanceAfterPayment;
            args.add(new Object[]{accountId, "PAYMENT", amount, "주문 결제 (#" + orderId + ")", orderId,
                    p.employeeUserId(), balanceAfterPayment, null, paymentTime, paymentTime});

            if ("REJECTED".equals(p.status())) {
                LocalDateTime refundTime = p.processDate() != null ? p.processDate() : paymentTime.plusHours(1);
                long balanceAfterRefund = balanceHolder[0] + amount;
                balanceHolder[0] = balanceAfterRefund;
                args.add(new Object[]{accountId, "REFUND", amount, "주문 반려 환불 (#" + orderId + ")", orderId,
                        adminUserId, balanceAfterRefund, "재고 부족으로 인한 반려 환불", refundTime, refundTime});
            }
        }
        return insertAndGetIds("deposit_history",
                "INSERT INTO `deposit_history` (`deposit_account_id`,`type`,`amount`,`description`,`related_order_id`," +
                        "`processed_by`,`balance_after`,`admin_memo`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?)",
                args);
    }
  
    private void appendHotItemUsageBurst(List<Long> hotItemIds, Map<Long, Integer> stockLedger,
                                          long adminUserId, List<Object[]> sink) {
        LocalDateTime now = LocalDateTime.now();
        for (Long itemId : hotItemIds) {
            int burstEvents = 15 + ThreadLocalRandom.current().nextInt(10); 
            for (int i = 0; i < burstEvents; i++) {
                int available = stockLedger.getOrDefault(itemId, 0);
                if (available <= 0) {
                    break; 
                }
                int qty = Math.min(available, 1 + ThreadLocalRandom.current().nextInt(3));
                int resultStock = available - qty;
                stockLedger.put(itemId, resultStock);
                LocalDateTime usageDate = now.minusDays(ThreadLocalRandom.current().nextInt(7))
                        .minusHours(ThreadLocalRandom.current().nextInt(24));
                sink.add(new Object[]{itemId, adminUserId, "인기 상품 판매 급증 (더미 데이터)", resultStock, usageDate, qty});
            }
        }
    }

    private void appendOrderDrivenStockUsage(List<OrderPlan> plans, Map<Long, Integer> stockLedger,
                                              long adminUserId, List<Object[]> sink) {
        for (OrderPlan p : plans) {
            if (!"COMPLETED".equals(p.status())) {
                continue;
            }
            int available = stockLedger.getOrDefault(p.itemId(), 0);
            int qty = Math.min(p.qty(), available);
            if (qty <= 0) {
                continue;
            }
            int resultStock = available - qty;
            stockLedger.put(p.itemId(), resultStock);
            LocalDateTime usageDate = p.processDate() != null ? p.processDate() : p.requestDate();
            sink.add(new Object[]{p.itemId(), adminUserId, "발주 승인에 따른 출고 (더미 데이터)", resultStock, usageDate, qty});
        }
    }

    private void reconcileItemStock(Map<Long, Integer> stockLedger) {
        List<Object[]> args = new ArrayList<>(stockLedger.size());
        for (Map.Entry<Long, Integer> e : stockLedger.entrySet()) {
            args.add(new Object[]{e.getValue(), e.getKey()});
        }
        chunkedBatchUpdate("UPDATE `item` SET `current_stock` = ? WHERE `item_id` = ?", args);
    }


    private InquiryGenResult insertInquiries(int inquiryCount, List<EmployeeInfo> employees, String runTag) {
        List<Object[]> args = new ArrayList<>(inquiryCount);
        List<Boolean> waitingFlags = new ArrayList<>(inquiryCount);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < inquiryCount; i++) {
            EmployeeInfo author = employees.get(ThreadLocalRandom.current().nextInt(employees.size()));

            boolean waiting = ThreadLocalRandom.current().nextInt(100) < 70;
            String topic = pick(INQUIRY_TOPICS);
            String title = "[" + topic + "] 문의드립니다 (" + runTag + "-" + i + ")";
            String content = faker.lorem().paragraph(2);
            LocalDateTime createdAt = waiting
                    ? now.minusMinutes(ThreadLocalRandom.current().nextInt(60 * 24 * 3)) // 최근 3일 이내 → 스케줄러가 우선 처리
                    : now.minusDays(3 + ThreadLocalRandom.current().nextInt(30));

            args.add(new Object[]{title, content, author.userId(), !waiting, null, null, createdAt, createdAt});
            waitingFlags.add(waiting);
        }

        List<Long> ids = insertAndGetIds("inquiries",
                "INSERT INTO `inquiries` (`title`,`content`,`user_id`,`is_read`,`ai_category`,`ai_draft_answer`,`created_at`,`updated_at`) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                args);
        return new InquiryGenResult(ids, waitingFlags);
    }

    private List<Long> insertInquiryComments(InquiryGenResult inquiries, long adminUserId) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < inquiries.inquiryIds().size(); i++) {
            if (inquiries.waitingFlags().get(i)) {
                continue; 
            }
            args.add(new Object[]{faker.lorem().sentence(15), inquiries.inquiryIds().get(i), adminUserId, now, now});
        }
        return insertAndGetIds("inquiry_comments",
                "INSERT INTO `inquiry_comments` (`content`,`inquiry_id`,`user_id`,`created_at`,`updated_at`) VALUES (?,?,?,?,?)",
                args);
    }



    private List<Long> insertCarts(List<EmployeeInfo> employees) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < employees.size(); i++) {
            if (i % 10 >= 7) {
                continue; 
            }
            args.add(new Object[]{employees.get(i).userId(), now, now});
        }
        return insertAndGetIds("cart", "INSERT INTO `cart` (`user_id`,`created_at`,`updated_at`) VALUES (?,?,?)", args);
    }

    private List<Long> insertCartDetails(List<Long> cartIds, List<Long> allItemIds) {
        List<Object[]> args = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long cartId : cartIds) {
            int itemCountInCart = 1 + ThreadLocalRandom.current().nextInt(4);
            Set<Long> chosen = new LinkedHashSet<>();
            int guard = 0;
            while (chosen.size() < itemCountInCart && guard < itemCountInCart * 5) {
                chosen.add(allItemIds.get(ThreadLocalRandom.current().nextInt(allItemIds.size())));
                guard++;
            }
            for (Long itemId : chosen) {
                args.add(new Object[]{cartId, itemId, 1 + ThreadLocalRandom.current().nextInt(5), false, now, now});
            }
        }
        return insertAndGetIds("cart_detail",
                "INSERT INTO `cart_detail` (`cart_id`,`item_id`,`quantity`,`deleted`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?)",
                args);
    }


    private List<Long> insertAnnualLeaves(int count, List<EmployeeInfo> employees, long adminUserId) {
        List<Object[]> args = new ArrayList<>(count);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            EmployeeInfo emp = employees.get(ThreadLocalRandom.current().nextInt(employees.size()));
            String type = pick(LEAVE_TYPES);
            LocalDate startDate = today.plusDays(ThreadLocalRandom.current().nextInt(61) - 30);
            int span = "ANNUAL".equals(type) ? ThreadLocalRandom.current().nextInt(3) : 0;
            LocalDate endDate = startDate.plusDays(span);

            int roll = ThreadLocalRandom.current().nextInt(100);
            String status;
            String rejectReason = null;
            Long processorId = null;
            LocalDateTime processedAt = null;
            if (roll < 40) {
                status = "PENDING";
            } else if (roll < 70) {
                status = "APPROVED";
                processorId = adminUserId;
                processedAt = now.minusDays(ThreadLocalRandom.current().nextInt(10));
            } else if (roll < 90) {
                status = "REJECTED";
                processorId = adminUserId;
                processedAt = now.minusDays(ThreadLocalRandom.current().nextInt(10));
                rejectReason = "업무 인수인계 미비로 반려";
            } else {
                status = "HOLD";
                processorId = adminUserId;
                processedAt = now.minusDays(ThreadLocalRandom.current().nextInt(5));
            }

            args.add(new Object[]{emp.userId(), startDate, endDate, type, pick(LEAVE_REASONS), status,
                    rejectReason, processorId, processedAt, now, now});
        }
        return insertAndGetIds("annual_leave",
                "INSERT INTO `annual_leave` (`user_id`,`start_date`,`end_date`,`type`,`reason`,`status`,`reject_reason`," +
                        "`processor_id`,`processed_at`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                args);
    }


    private List<Long> insertChargeRequests(int count, List<EmployeeInfo> employees, long adminUserId) {
        List<Object[]> args = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            EmployeeInfo emp = employees.get(ThreadLocalRandom.current().nextInt(employees.size()));
            long amount = (1 + ThreadLocalRandom.current().nextInt(20)) * 100_000L;
            LocalDateTime requestDate = now.minusDays(ThreadLocalRandom.current().nextInt(30));

            int roll = ThreadLocalRandom.current().nextInt(100);
            String status;
            LocalDateTime processDate = null;
            Long processorId = null;
            String rejectReason = null;
            if (roll < 40) {
                status = "PENDING";
            } else if (roll < 80) {
                status = "APPROVED";
                processDate = requestDate.plusHours(2);
                processorId = adminUserId;
            } else {
                status = "REJECTED";
                processDate = requestDate.plusHours(2);
                processorId = adminUserId;
                rejectReason = "입금 확인 불가";
            }

            args.add(new Object[]{emp.userId(), amount, status, requestDate, processDate, processorId, rejectReason});
        }
        return insertAndGetIds("charge_request",
                "INSERT INTO `charge_request` (`user_id`,`amount`,`status`,`request_date`,`process_date`,`processor_id`,`reject_reason`) " +
                        "VALUES (?,?,?,?,?,?,?)",
                args);
    }
}
