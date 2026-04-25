package com.jstudy.inout.stock.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.admin.*;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.entity.*;
import com.jstudy.inout.stock.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 재고 관리 서비스.
 *
 * [주요 기능]
 * - 상품 등록 / 수정 / 논리 삭제
 * - 재고 입고 처리 및 이력 기록
 * - 재고 조정 (비관적 락 적용)
 * - 통합 이력 조회 (입고 + 사용, 페이징)
 * - 저재고 / 품절 알림 목록 조회
 * - 재고 상세 조회 (이력 + 집계)
 *
 * 클래스 레벨 @Transactional(readOnly = true) 기본 설정.
 * 쓰기 작업이 있는 메서드는 개별 @Transactional 로 덮어씁니다.
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class StockAdmService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final StockReceivingHistoryRepository receivingHistoryRepository;
    private final UserRepository userRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;

    /**
     * 신규 상품을 등록합니다.
     *
     * [처리 순서]
     * 1. 카테고리 존재 여부 확인
     * 2. 상품명 중복 체크
     * 3. DTO → Entity 변환 후 저장 (초기 재고: 0)
     *
     * @param stockRegister 상품 등록 요청 DTO
     * @return 등록된 상품 ID
     * @throws InoutException 카테고리 없음(404) / 상품명 중복(400)
     */
    @Transactional
    public Long registerStock(StockRegister stockRegister) {
        log.info("상품 등록 시도: name={}, categoryId={}", stockRegister.getName(), stockRegister.getCategoryId());

        // 카테고리 유효성 검증
        ItemCategory category = itemCategoryRepository.findById(stockRegister.getCategoryId())
                .orElseThrow(() -> new InoutException("해당 카테고리를 찾을 수 없습니다.", 404, "CATEGORY_NOT_FOUND"));

        // 상품명 중복 체크
        if (itemRepository.existsByName(stockRegister.getName())) {
            throw new InoutException("이미 등록된 상품명입니다.", 400, "DUPLICATE_ITEM_NAME");
        }

        Item item = stockRegister.toEntity(category);
        Item savedItem = itemRepository.save(item);

        log.info("상품 등록 성공: id={}, name={}", savedItem.getItemId(), savedItem.getName());
        return savedItem.getItemId();
    }

    /**
     * 상품 기본 정보를 수정합니다.
     * JPA Dirty Checking으로 save() 없이 트랜잭션 종료 시 자동 반영됩니다.
     *
     * @param itemId      수정 대상 상품 ID
     * @param stockUpdate 수정 요청 DTO
     * @param userId      수정 처리 관리자 ID (감사 로그용 — 현재 미사용, 추후 확장 가능)
     * @throws InoutException 상품 없음(404) / 카테고리 없음(404)
     */
    @Transactional
    public void updateStock(Long itemId, StockUpdate stockUpdate, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        ItemCategory category = itemCategoryRepository.findById(stockUpdate.getCategoryId())
                .orElseThrow(() -> new InoutException("카테고리를 찾을 수 없습니다.", 404, "CATEGORY_NOT_FOUND"));

        // 엔티티 내부 도메인 메서드로 값 변경 → Dirty Checking 동작
        item.updateInfo(
                stockUpdate.getName(),
                category,
                stockUpdate.getUnitPrice(),
                stockUpdate.getMinStockLevel(),
                stockUpdate.getUnitDescription(),
                stockUpdate.getDescription()
        );
    }

    /**
     * 상품을 논리 삭제합니다.
     * 실제 DB 레코드는 삭제하지 않고 deleted=true로 표시합니다.
     *
     * @param itemId 삭제 대상 상품 ID
     * @throws InoutException 상품 없음(404) / 이미 삭제된 상품(400)
     */
    @Transactional
    public void deleteStock(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("삭제하려는 상품이 존재하지 않습니다.", 404, "ITEM_NOT_FOUND"));

        if (item.getDeleted()) {
            throw new InoutException("이미 삭제된 상품입니다.", 400, "ALREADY_DELETED");
        }

        // 도메인 메서드 호출 → deleted=true, deletedAt=now() 설정
        item.delete();
    }

    /**
     * 관리자용 재고 목록을 페이징 조회합니다.
     * 이름 검색 여부에 따라 쿼리를 분기합니다.
     *
     * @param name     상품명 검색어 (null 또는 공백이면 전체 조회)
     * @param deleted  삭제 여부 필터 (false: 활성 상품, true: 삭제된 상품)
     * @param pageable 페이지 정보
     * @return 페이징된 상품 응답 목록
     */
    @Transactional(readOnly = true)
    public Page<StockAdminResponse> getAdminStockList(String name, boolean deleted, Pageable pageable) {
        Page<Item> stockPage;

        if (StringUtils.hasText(name)) {
            // 이름 포함 검색 + 삭제 여부 필터
            stockPage = itemRepository.findByNameContainingAndDeleted(name, deleted, pageable);
        } else {
            // 삭제 여부 필터만 적용
            stockPage = itemRepository.findByDeleted(deleted, pageable);
        }

        return stockPage.map(StockAdminResponse::from);
    }

    /**
     * 재고 입고를 처리하고 입고 이력을 기록합니다.
     *
     * [처리 순서]
     * 1. 상품 조회
     * 2. 작업자(관리자) 조회
     * 3. 재고 증가 (item.addStock())
     * 4. 입고 이력 생성 및 저장
     *
     * @param itemId   입고 대상 상품 ID
     * @param quantity 입고 수량
     * @param userId   처리 관리자 ID
     * @param memo     메모 (선택)
     * @return 입고된 상품 ID
     */
    @Transactional
    public Long receiveStock(Long itemId, int quantity, Long userId, String memo) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        // 재고 증가 (도메인 메서드)
        item.addStock(quantity);

        // 입고 이력 저장 — resultStock에 증가 후 최종 재고를 기록
        StockReceivingHistory history = StockReceivingHistory.builder()
                .item(item)
                .user(user)
                .receivingQuantity(quantity)
                .resultStock(item.getCurrentStock()) // 반영 후 최종 재고
                .memo(memo)
                .build();

        receivingHistoryRepository.save(history);
        return item.getItemId();
    }

    /**
     * 특정 상품의 통합 이력(입고 + 사용)을 페이징 조회합니다.
     *
     * 현재 구현: Java 메모리에서 두 이력을 합쳐 날짜 역순으로 정렬 후 skip/limit로 페이징.
     * 개선 가능: Native Query UNION으로 DB 레벨 정렬 및 페이징 처리 (데이터 증가 시 권장)
     *
     * @param itemId 조회 대상 상품 ID
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     * @return 페이징된 통합 이력 목록
     */
    @Transactional(readOnly = true)
    public List<StockHistoryResponse> getUnifiedHistory(Long itemId, int page, int size) {
        // 입고 이력과 사용 이력을 각각 전체 조회
        List<StockReceivingHistory> receiving = receivingHistoryRepository.findAllByItem_ItemId(itemId);
        List<StockUsageHistory> usage = usageHistoryRepository.findAllByItem_ItemId(itemId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        // 날짜 역순(최신순) 정렬 후 페이징
        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * 집계(totalReceived, totalUsed) 계산을 위한 전체 이력 조회.
     * 페이징 없이 모든 데이터를 반환합니다.
     * getStockDetail() 메서드에서 내부적으로 사용됩니다.
     *
     * @param itemId 조회 대상 상품 ID
     * @return 페이징 없는 전체 이력 목록
     */
    private List<StockHistoryResponse> getAllHistoryForStats(Long itemId) {
        List<StockReceivingHistory> receiving = receivingHistoryRepository.findAllByItem_ItemId(itemId);
        List<StockUsageHistory> usage = usageHistoryRepository.findAllByItem_ItemId(itemId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));
        return combined;
    }

    /**
     * 저재고 알림 대상 상품 목록을 조회합니다.
     * currentStock <= minStockLevel 조건 (단, 삭제되지 않은 상품).
     *
     * @return 저재고 상품 목록
     */
    public List<StockAdminResponse> getLowStockAlerts() {
        return itemRepository.findLowStockItems().stream()
                .map(StockAdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 품절 상품 목록을 조회합니다.
     * currentStock = 0 조건 (단, 삭제되지 않은 상품).
     *
     * @return 품절 상품 목록
     */
    public List<StockAdminResponse> getOutOfStockItems() {
        return itemRepository.findOutOfStockItems().stream()
                .map(StockAdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 상세 정보와 이력(페이징), 총 입고량/사용량을 조합하여 반환합니다.
     *
     * 화면 표시용 이력(pagedHistory)과 집계용 전체 이력(allHistory)을
     * 분리하여 처리합니다. 집계는 전체 데이터 기준이어야 정확하기 때문입니다.
     *
     * @param itemId 조회 대상 상품 ID
     * @param page   이력 페이지 번호
     * @param size   이력 페이지 크기
     * @return 상세 정보 응답 DTO
     */
    @Transactional(readOnly = true)
    public StockDetailResponse getStockDetail(Long itemId, int page, int size) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        // 화면 표시용: 페이징 적용된 이력
        List<StockHistoryResponse> pagedHistory = getUnifiedHistory(itemId, page, size);

        // 집계용: 전체 이력 (totalReceived, totalUsed 계산에 사용)
        List<StockHistoryResponse> allHistory = getAllHistoryForStats(itemId);

        // 재고 상태 판단
        String status = "정상";
        if (item.getCurrentStock() == 0) status = "품절";
        else if (item.getCurrentStock() <= item.getMinStockLevel()) status = "저재고";

        return StockDetailResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getName())
                .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정")
                .currentStock(item.getCurrentStock())
                .minStockLevel(item.getMinStockLevel())
                .status(status)
                .history(pagedHistory)           // 페이징된 이력 (화면 표시용)
                .totalReceived(allHistory.stream()  // 전체 기준 입고 합계
                        .filter(h -> h.getType().equals("입고"))
                        .mapToLong(StockHistoryResponse::getQuantity)
                        .sum())
                .totalUsed(allHistory.stream()      // 전체 기준 사용 합계
                        .filter(h -> h.getType().equals("사용"))
                        .mapToLong(h -> Math.abs(h.getQuantity()))
                        .sum())
                .build();
    }

    /**
     * 실제 재고 수량을 기준으로 DB 재고를 조정합니다 (재고 실사 기능).
     *
     * 비관적 락(findByIdWithLock)을 사용하여 동시 수정을 방지합니다.
     * 차이(diff)에 따라 입고 또는 사용 이력으로 기록합니다:
     * - diff > 0: 입고 이력으로 기록
     * - diff < 0: 사용 이력으로 기록 (재고 조정 사유 포함)
     * - diff = 0: 변경 없음, 즉시 반환
     *
     * @param adminId 조정 처리 관리자 ID
     * @param request 조정 요청 DTO (itemId, actualStock, reason)
     */
    @Transactional
    public void adjustStock(Long adminId, StockAdmRequest request) {
        // 비관적 락으로 조회 — 트랜잭션 종료 전까지 다른 트랜잭션의 쓰기 차단
        Item item = itemRepository.findByIdWithLock(request.getItemId())
                .orElseThrow(() -> new InoutException("상품 없음"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보 없음"));

        // 변경하려는 수량 - 현재 수량 = 차이
        int diff = request.getActualStock() - item.getCurrentStock();

        if (diff == 0) {
            return; // 변경사항 없음
        }

        if (diff > 0) {
            // 재고 증가 → 입고 이력으로 기록
            StockReceivingHistory receiving = StockReceivingHistory.builder()
                    .item(item)
                    .user(admin)
                    .receivingQuantity(diff)
                    .resultStock(request.getActualStock())
                    .memo("[재고조정] " + request.getReason())
                    .build();

            receivingHistoryRepository.save(receiving);
            item.addStock(diff);

        } else {
            // 재고 감소 → 사용 이력으로 기록 (절대값 변환)
            int usageQuantity = Math.abs(diff);

            StockUsageHistory usage = StockUsageHistory.builder()
                    .item(item)
                    .user(admin)
                    .usageQuantity(usageQuantity)
                    .resultStock(request.getActualStock())
                    .memo("[재고조정] " + request.getReason())
                    .build();

            usageHistoryRepository.save(usage);
            item.removeStock(usageQuantity);
        }
    }
}