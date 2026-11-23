package com.jstudy.inout.stock.service;

import com.jstudy.inout.notification.entity.NotificationType;
import com.jstudy.inout.notification.service.NotificationEvent.Target;
import com.jstudy.inout.notification.service.Notifier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.count.StockCountApplyResponse;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import com.jstudy.inout.stock.dto.count.StockCountSubmitRequest;
import com.jstudy.inout.stock.dto.count.StockCountSummaryResponse;
import com.jstudy.inout.stock.entity.StockCountLine;
import com.jstudy.inout.stock.entity.StockCountReason;
import com.jstudy.inout.stock.entity.StockCountSheet;
import com.jstudy.inout.stock.entity.StockCountSheet.AiReading;
import com.jstudy.inout.stock.entity.StockCountStatus;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.entity.StoreStockHistory;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매장 재고 실사표: 만들기 → (AI 읽기) → 직원 제출 → 점주 승인(매장 재고 반영) / 반려.
 *
 * 반영 수량은 "실재고 - 출력 시점 매장 재고"(차이)이고, 이 차이를 지금 매장 재고에 더한다.
 * 출력 후 승인까지 사이에 들어온 수령·사용 기록을 덮어쓰지 않기 위해서다.
 * (실재고로 그대로 덮어쓰면, 그사이 사용한 수량이 다시 살아난다.)
 *
 * 모든 조회·처리는 로그인한 사용자의 소속 매장 실사표로 제한한다 (CROSS_STORE_FORBIDDEN).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockCountService {

    /** "최근 사용 상품"으로 볼 기간 */
    static final long RECENT_USE_HOURS = 24;
    private static final int LIST_LIMIT = 30;
    private static final DateTimeFormatter CODE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd-HHmm");

    private final StockCountSheetRepository sheetRepository;
    private final StoreStockRepository storeStockRepository;
    private final StoreStockHistoryRepository historyRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Notifier notifier;

    // ──────────────────────────────────────────────────────────────────
    // 만들기
    // ──────────────────────────────────────────────────────────────────

    /** 직원·점주가 [지금 만들기]를 누른 경우: 소속 매장 실사표를 만든다. */
    @Transactional
    public Long createSheet(Long userId) {
        User creator = loadUser(userId);
        return createSheetForStore(creator.getStore() == null ? null : creator.getStore().getId(), creator);
    }

    /**
     * 매장 실사표를 만든다. 대상은 매장 재고가 안전재고 이하인 상품과, 최근 24시간 안에 사용 처리한 상품이다.
     * 줄은 매장 보관 위치 순서로 정렬해 직원이 매장을 한 방향으로 돌며 셀 수 있게 한다.
     *
     * @param creator 만든 사람. 스케줄러가 만들면 null.
     */
    @Transactional
    public Long createSheetForStore(Long storeId, User creator) {
        if (storeId == null) {
            throw new InoutException(ErrorCode.STORE_REQUIRED);
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new InoutException(ErrorCode.STORE_NOT_FOUND));

        Map<Long, StockCountReason> reasonById = new LinkedHashMap<>();
        Map<Long, StoreStock> stockById = new HashMap<>();
        for (StoreStock s : storeStockRepository.findLowStockByStore(storeId)) {
            reasonById.put(s.getId(), StockCountReason.LOW_STOCK);
            stockById.put(s.getId(), s);
        }
        List<Long> recentlyUsed = historyRepository.findStoreStockIdsChangedSince(
                        storeId, StoreStockChangeType.USE, LocalDateTime.now().minusHours(RECENT_USE_HOURS)).stream()
                .filter(id -> !reasonById.containsKey(id))
                .toList();
        for (StoreStock s : storeStockRepository.findAllById(recentlyUsed)) {
            if (Boolean.TRUE.equals(s.getItem().getDeleted())) {
                continue;
            }
            reasonById.put(s.getId(), StockCountReason.RECENT_USE);
            stockById.put(s.getId(), s);
        }

        if (reasonById.isEmpty()) {
            throw new InoutException(ErrorCode.STOCK_COUNT_NO_TARGET);
        }

        List<StoreStock> ordered = stockById.values().stream()
                .sorted(Comparator
                        .comparing((StoreStock s) -> s.getLocation() == null ? null : s.getLocation().toUpperCase(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(s -> s.getItem().getName()))
                .toList();

        List<StockCountLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (StoreStock s : ordered) {
            lines.add(StockCountLine.builder()
                    .lineNo(lineNo++)
                    .storeStock(s)
                    .reason(reasonById.get(s.getId()))
                    .build());
        }

        StockCountSheet sheet = sheetRepository.save(
                StockCountSheet.create(nextSheetCode(storeId), store, creator, lines));
        log.info("[재고 실사] 실사표 생성: id={}, storeId={}, code={}, 품목 {}개",
                sheet.getId(), storeId, sheet.getSheetCode(), lines.size());
        if (creator == null) {
            // 아침 자동 생성일 때만 알린다 (직접 만든 사람은 이미 화면을 보고 있다).
            notifier.send(NotificationType.STOCK_COUNT_READY, "오늘 재고 실사표가 준비되었습니다",
                    lines.size() + "품목 · 영수증을 출력해 실재고를 적어 주세요.", null,
                    Target.storeEmployees(storeId, "/emp/stock-counts"));
        }
        return sheet.getId();
    }

    /** 실사표가 있는 매장 목록 (스케줄러용) */
    public List<Long> storeIdsWithStock() {
        return storeStockRepository.findStoreIdsWithStock();
    }

    /** SC{매장ID}-251123-0900 형식. 같은 분에 여러 장을 만들면 -2, -3을 붙인다. */
    private String nextSheetCode(Long storeId) {
        String base = "SC" + storeId + "-" + LocalDateTime.now().format(CODE_FORMAT);
        String code = base;
        for (int seq = 2; sheetRepository.existsBySheetCode(code); seq++) {
            code = base + "-" + seq;
        }
        return code;
    }

    // ──────────────────────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────────────────────

    public List<StockCountSummaryResponse> getRecentSheets(Long userId, StockCountStatus status) {
        Long storeId = StoreStockService.storeIdOf(loadUser(userId));
        PageRequest page = PageRequest.of(0, LIST_LIMIT);
        List<StockCountSheet> sheets = status == null
                ? sheetRepository.findAllByStore_IdOrderByIdDesc(storeId, page)
                : sheetRepository.findAllByStore_IdAndStatusOrderByIdDesc(storeId, status, page);
        return sheets.stream().map(StockCountSummaryResponse::from).toList();
    }

    /** 직원 화면·영수증 출력용. 지금 재고는 싣지 않는다(영수증에는 출력 시점 수량만 찍는다). */
    public StockCountSheetResponse getSheet(Long userId, Long sheetId) {
        return StockCountSheetResponse.from(findOwnStoreSheet(userId, sheetId), Map.of());
    }

    /** 점주 확인 화면용. 매장 재고 행별 지금 수량과 승인 후 예상 수량을 함께 싣는다. */
    public StockCountSheetResponse getSheetForReview(Long ownerId, Long sheetId) {
        StockCountSheet sheet = findOwnStoreSheet(ownerId, sheetId);
        Map<Long, Integer> currentQty = new HashMap<>();
        for (StockCountLine line : sheet.getLines()) {
            currentQty.put(line.getStoreStock().getId(), line.getStoreStock().getQuantity());
        }
        return StockCountSheetResponse.from(sheet, currentQty);
    }

    /** AI에 넘길 줄 목록. 작성 중(OPEN)인 소속 매장 실사표만 사진을 받을 수 있다. */
    public StockCountSheetResponse getSheetForScan(Long userId, Long sheetId) {
        StockCountSheet sheet = findOwnStoreSheet(userId, sheetId);
        sheet.requireStatus(StockCountStatus.OPEN);
        return StockCountSheetResponse.from(sheet, Map.of());
    }

    private StockCountSheet findOwnStoreSheet(Long userId, Long sheetId) {
        StockCountSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new InoutException(ErrorCode.STOCK_COUNT_SHEET_NOT_FOUND));
        assertSameStore(loadUser(userId), sheet);
        return sheet;
    }

    // ──────────────────────────────────────────────────────────────────
    // AI 읽기 결과 기록 · 직원 제출
    // ──────────────────────────────────────────────────────────────────

    /** AI가 읽은 값을 참고용으로 저장한다. 재고는 바꾸지 않는다. (매장 확인은 getSheetForScan에서 끝냈다) */
    @Transactional
    public void recordAiReading(Long sheetId, Map<Integer, AiReading> readings, Boolean sheetCodeMatched, String photoUrl) {
        StockCountSheet sheet = sheetRepository.findByIdWithLock(sheetId)
                .orElseThrow(() -> new InoutException(ErrorCode.STOCK_COUNT_SHEET_NOT_FOUND));
        sheet.recordAiReading(readings, sheetCodeMatched, photoUrl);
    }

    @Transactional
    public void submit(Long sheetId, Long employeeId, StockCountSubmitRequest request) {
        StockCountSheet sheet = sheetRepository.findByIdWithLock(sheetId)
                .orElseThrow(() -> new InoutException(ErrorCode.STOCK_COUNT_SHEET_NOT_FOUND));
        User employee = loadUser(employeeId);
        assertSameStore(employee, sheet);

        Map<Long, Integer> counts = new HashMap<>();
        for (StockCountSubmitRequest.Line line : request.getLines()) {
            counts.put(line.getLineId(), line.getCountedQty());
        }
        sheet.submit(employee, counts);
        notifier.send(NotificationType.STOCK_COUNT_SUBMITTED, "재고 실사가 제출되었습니다 (" + sheet.getSheetCode() + ")",
                employee.getName() + " 님 제출 · 확인 후 승인해 주세요.", employeeId,
                Target.storeOwners(sheet.getStore().getId(), "/owner/stock-counts"));
        log.info("[재고 실사] 제출: id={}, userId={}", sheetId, employeeId);
    }

    // ──────────────────────────────────────────────────────────────────
    // 점주 승인 · 반려
    // ──────────────────────────────────────────────────────────────────

    /**
     * 제출된 실재고를 매장 재고에 반영한다.
     * 1) 차이가 있는 매장 재고를 itemId 오름차순으로 모두 락을 잡고(수령 확인과 같은 순서), 반영 후 0 이상인지 전부 검사한다.
     * 2) 전부 통과했을 때만 재고를 바꾸고 이력을 남긴다. 하나라도 실패하면 아무것도 바뀌지 않는다.
     */
    @Transactional
    public StockCountApplyResponse approve(Long sheetId, Long ownerId) {
        // 같은 실사표를 두 번 승인해도 한 번만 반영되도록 실사표 행부터 잠근다.
        StockCountSheet sheet = sheetRepository.findByIdWithLock(sheetId)
                .orElseThrow(() -> new InoutException(ErrorCode.STOCK_COUNT_SHEET_NOT_FOUND));
        User owner = loadUser(ownerId);
        assertSameStore(owner, sheet);
        sheet.requireStatus(StockCountStatus.SUBMITTED);
        Long storeId = sheet.getStore().getId();

        List<StockCountLine> changed = sheet.getLines().stream()
                .filter(l -> l.getDiff() != null && l.getDiff() != 0)
                .sorted(Comparator.comparing(StockCountLine::getItemId))
                .toList();

        // 1) 검사: 락을 잡고 최신 수량을 다시 읽은 뒤, 반영 후 음수가 되는 상품이 있는지 본다.
        Map<Long, StoreStock> locked = new HashMap<>();
        for (StockCountLine line : changed) {
            StoreStock stock = storeStockRepository.findByStoreAndItemWithLock(storeId, line.getItemId())
                    .orElseThrow(() -> new InoutException(ErrorCode.STORE_STOCK_NOT_FOUND));
            // 실사표를 읽을 때 같은 행이 1차 캐시에 올라와 있어, 락을 잡은 뒤 최신 값으로 다시 읽는다.
            entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE);
            if (stock.getQuantity() + line.getDiff() < 0) {
                throw new InoutException(ErrorCode.STOCK_COUNT_NEGATIVE_RESULT, String.format(
                        "'%s'은(는) 지금 매장 재고가 %d개라 %d개를 빼면 0보다 작아집니다. 다시 세어 제출해 달라고 반려해 주세요.",
                        line.getItemName(), stock.getQuantity(), -line.getDiff()));
            }
            locked.put(line.getItemId(), stock);
        }

        // 2) 반영
        int increased = 0;
        int decreased = 0;
        for (StockCountLine line : changed) {
            StoreStock stock = locked.get(line.getItemId());
            int diff = line.getDiff();
            if (diff > 0) {
                stock.increase(diff);
                increased++;
            } else {
                stock.decrease(-diff);
                decreased++;
            }
            historyRepository.save(StoreStockHistory.builder()
                    .storeStock(stock).user(owner).type(StoreStockChangeType.COUNT)
                    .changeQty(diff)
                    .memo(String.format("[실사 %s] 출력 시점 %d → 실재고 %d",
                            sheet.getSheetCode(), line.getSystemQty(), line.getCountedQty()))
                    .build());
            line.markApplied(diff);
        }

        int unchanged = 0;
        for (StockCountLine line : sheet.getLines()) {
            if (line.getDiff() != null && line.getDiff() == 0) {
                line.markApplied(0);
                unchanged++;
            }
        }

        sheet.markApplied(owner);
        if (sheet.getSubmittedBy() != null) {
            notifier.send(NotificationType.STOCK_COUNT_APPLIED, "재고 실사가 승인되어 반영되었습니다 (" + sheet.getSheetCode() + ")",
                    "늘어남 " + increased + " · 줄어듦 " + decreased + " · 일치 " + unchanged + "품목", ownerId,
                    Target.user(sheet.getSubmittedBy().getId(), "/emp/stock-counts"));
        }
        log.info("[재고 실사] 승인: id={}, storeId={}, 증가 {} / 감소 {} / 일치 {}",
                sheetId, storeId, increased, decreased, unchanged);
        return new StockCountApplyResponse(increased, decreased, unchanged);
    }

    @Transactional
    public void reject(Long sheetId, Long ownerId, String reason) {
        StockCountSheet sheet = sheetRepository.findByIdWithLock(sheetId)
                .orElseThrow(() -> new InoutException(ErrorCode.STOCK_COUNT_SHEET_NOT_FOUND));
        User owner = loadUser(ownerId);
        assertSameStore(owner, sheet);
        sheet.reject(owner, reason.trim());
        if (sheet.getSubmittedBy() != null) {
            notifier.send(NotificationType.STOCK_COUNT_REJECTED, "재고 실사가 반려되었습니다 (" + sheet.getSheetCode() + ")",
                    "사유를 확인하고 다시 세어 제출해 주세요.", ownerId,
                    Target.user(sheet.getSubmittedBy().getId(), "/emp/stock-counts"));
        }
        log.info("[재고 실사] 반려: id={}", sheetId);
    }

    // ──────────────────────────────────────────────────────────────────

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));
    }

    private static void assertSameStore(User user, StockCountSheet sheet) {
        if (!StoreStockService.storeIdOf(user).equals(sheet.getStore().getId())) {
            throw new InoutException(ErrorCode.CROSS_STORE_FORBIDDEN);
        }
    }
}
