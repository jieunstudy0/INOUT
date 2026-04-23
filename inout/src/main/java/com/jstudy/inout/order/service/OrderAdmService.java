package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.order.dto.*;
import com.jstudy.inout.order.dto.OrderProcessRequest.ItemStatusUpdate;
import com.jstudy.inout.order.entity.*;
import com.jstudy.inout.order.repository.*;
import com.jstudy.inout.stock.entity.*;
import com.jstudy.inout.stock.exception.NotEnoughStockException;
import com.jstudy.inout.stock.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;

/**
 * 관리자용 발주 처리 서비스.
 *
 * [주요 기능]
 * - 발주 목록 조회 (상태별 필터링)
 * - 발주 상세 항목별 처리 (승인/반려/지연)
 * - 일괄 승인 (독립 트랜잭션으로 부분 실패 허용)
 * - 발주 처리 결과 이메일 알림 (비동기)
 * - 발주 내역 엑셀 다운로드
 *
 * 일괄 승인 시 각 발주는 REQUIRES_NEW 트랜잭션으로 독립 처리됩니다.
 * 하나가 실패해도 다른 발주는 정상 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderAdmService {

    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final MailComponent mailComponent;
    private final OrderApprovalTxService orderApprovalTxService;

    /**
     * 발주 목록을 상태별로 조회합니다.
     *
     * @param status null이면 전체 조회, 값이 있으면 해당 상태 필터링
     * @return 발주 목록 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<OrderAdminResponse> getAllOrders(OrderStatus status) {
        List<OrderRequest> orders;

        if (status == null) {
            orders = orderRequestRepository.findAllByOrderByRequestDateDesc();
        } else {
            orders = orderRequestRepository.findAllByStatusOrderByRequestDateDesc(status);
        }

        return orders.stream().map(order -> {
            // 대표 상품명: 첫 번째 품목 이름 (없으면 "상품 없음")
            String repItemName = "상품 없음";
            int itemCount = 0;

            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                repItemName = order.getOrderDetails().get(0).getItem().getName();
                itemCount = order.getOrderDetails().size();
            }

            return OrderAdminResponse.builder()
                    .orderRequestId(order.getId())
                    .storeName(order.getRequestUser().getStore().getName())
                    .employeeName(order.getRequestUser().getName())
                    .requestDate(order.getRequestDate())
                    .status(order.getStatus())
                    .totalPrice(order.getTotalPrice())
                    .representativeItemName(repItemName)
                    .itemCount(itemCount)
                    .build();

        }).collect(Collectors.toList());
    }

    /**
     * 발주 상세 항목들을 개별적으로 처리합니다 (승인/반려/지연).
     *
     * [처리 흐름]
     * 1. 각 OrderDetail 상태 업데이트
     * 2. 승인(APPROVED) 항목은 재고 차감 + 사용 이력 기록
     * 3. 모든 항목이 처리 완료이면 COMPLETED, 미처리 항목이 있으면 PARTIAL
     * 4. 처리 완료 후 담당 직원에게 이메일 알림 발송 (비동기)
     *
     * @param orderId 처리 대상 발주 ID
     * @param request 상세 항목별 처리 요청
     * @param adminId 처리 관리자 ID
     */
    @Transactional
    public void processOrderItems(Long orderId, OrderProcessRequest request, Long adminId) {
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InoutException("처리할 발주 상세 항목이 없습니다.", 400, "EMPTY_ORDER_ITEMS");
        }

        boolean hasDelayedOrRejected = false;

        for (ItemStatusUpdate update : request.items()) {
            if (update == null || update.orderDetailId() == null || update.status() == null) {
                throw new InoutException("발주 상세 처리 요청 값이 올바르지 않습니다.", 400, "INVALID_REQUEST");
            }

            OrderDetail detail = orderDetailRepository
                    .findByOrderDetailIdAndOrderRequest_Id(update.orderDetailId(), orderId)
                    .orElseThrow(() -> new InoutException(
                            "해당 주문에 속한 발주 상세 항목을 찾을 수 없습니다.", 404, "ORDER_DETAIL_NOT_FOUND"));

            validateStatusTransition(detail.getStatus(), update.status());

            // 승인 항목만 재고 차감 처리
            if (update.status().needsStockDeduction() && detail.getStatus() != OrderDetailStatus.APPROVED) {
                approveItemStock(detail, adminUser, orderId);
            }

            if (detail.getStatus() != update.status()) {
                detail.updateStatus(update.status());
            }

            // 미처리 항목 존재 여부 확인
            if (!update.status().isProcessed()) {
                hasDelayedOrRejected = true;
            }
        }

        // 주문 마스터 상태 업데이트
        updateOrderStatus(order, hasDelayedOrRejected);

        // 비동기 이메일 알림 발송 (실패해도 트랜잭션에 영향 없음)
        mailComponent.sendOrderStateEmail(order);
    }

    /**
     * 여러 발주를 일괄 승인합니다.
     *
     * 각 발주는 독립 트랜잭션(REQUIRES_NEW)으로 처리되어,
     * 하나가 재고 부족으로 실패해도 나머지는 정상 처리됩니다.
     * 실패한 발주는 자동으로 반려 처리되고 결과에 포함됩니다.
     *
     * @param request 일괄 승인 요청 (발주 ID 목록)
     * @param adminId 처리 관리자 ID
     * @return 성공/실패 건수 및 실패 목록
     */
    public BulkOrderResponse bulkApproveOrders(BulkOrderRequest request, Long adminId) {
        int successCount = 0;
        List<BulkOrderResponse.FailedOrder> failures = new ArrayList<>();

        for (Long orderId : request.getOrderIds()) {
            try {
                // REQUIRES_NEW: 별도 트랜잭션으로 처리 → 실패해도 다른 발주에 영향 없음
                orderApprovalTxService.processSingleOrderApproval(orderId, adminId);
                successCount++;

            } catch (NotEnoughStockException e) {
                // 재고 부족 시 자동 반려
                orderApprovalTxService.processSingleOrderRejection(orderId, "자동 반려: " + e.getMessage());
                failures.add(BulkOrderResponse.FailedOrder.builder()
                        .orderId(orderId)
                        .reason("재고 부족")
                        .build());
            } catch (Exception e) {
                failures.add(BulkOrderResponse.FailedOrder.builder()
                        .orderId(orderId)
                        .reason("시스템 오류: " + e.getMessage())
                        .build());
            }
        }

        return BulkOrderResponse.builder()
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }

    /**
     * 발주 항목 승인 시 재고를 차감하고 사용 이력을 기록합니다.
     * 비관적 락(findByIdWithLock)으로 동시 차감을 방지합니다.
     *
     * @param detail    처리 대상 주문 상세 항목
     * @param adminUser 처리 관리자
     * @param orderId   주문 ID (이력 메모에 기록)
     * @throws NotEnoughStockException 재고 부족 시
     */
    private void approveItemStock(OrderDetail detail, User adminUser, Long orderId) {
        // 비관적 락으로 재고 조회 → 동시 차감 방지
        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보 없음", 404, "ITEM_NOT_FOUND"));

        // 재고 차감 (부족 시 NotEnoughStockException 발생)
        item.removeStock(detail.getRequestQuantity());

        StockUsageHistory usage = StockUsageHistory.builder()
                .item(item)
                .user(adminUser)
                .usageQuantity(detail.getRequestQuantity())
                .resultStock(item.getCurrentStock())
                .memo("발주 승인 (주문번호: " + orderId + ")")
                .build();
        usageHistoryRepository.save(usage);
    }

    /**
     * 발주 처리 결과에 따라 주문 마스터 상태를 업데이트합니다.
     *
     * @param order    업데이트 대상 주문
     * @param hasIssue 미처리/반려 항목 존재 여부
     */
    private void updateOrderStatus(OrderRequest order, boolean hasIssue) {
        // 미처리 항목이 있으면 PARTIAL(부분처리), 없으면 COMPLETED
        if (hasIssue) order.updateStatus(OrderStatus.PARTIAL);
        else order.updateStatus(OrderStatus.COMPLETED);
        order.updateProcessDate(LocalDateTime.now());
    }

    private void validateStatusTransition(OrderDetailStatus current, OrderDetailStatus target) {
        if (current == target) {
            return;
        }
        if (!current.canUpdate()) {
            throw new InoutException("이미 처리된 발주 상세 항목은 상태를 변경할 수 없습니다.", 400, "INVALID_STATUS");
        }
        if (target == OrderDetailStatus.WAITING) {
            throw new InoutException("대기 상태로 되돌릴 수 없습니다.", 400, "INVALID_STATUS");
        }
    }

    /**
     * 전체 발주 내역을 엑셀 파일로 내보냅니다.
     *
     * 헤더 스타일(굵은 글씨, 노란 배경)과 열 자동 너비를 적용합니다.
     * 파일명은 "발주내역리스트_YYYYMMDD.xlsx" 형식으로 생성됩니다.
     *
     * @param response 파일 다운로드를 위한 HttpServletResponse
     * @throws IOException 파일 쓰기 실패 시
     */
    @Transactional(readOnly = true)
    public void exportOrdersToExcel(HttpServletResponse response) throws IOException {
        List<OrderRequest> orders = orderRequestRepository.findAllByOrderByRequestDateDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("발주 내역");

        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {"주문번호", "매장명", "신청자", "신청일시", "상태", "총 금액", "반려 사유"};

        // 헤더 스타일 설정 (굵은 글씨 + 노란 배경)
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 데이터 행 생성
        int rowNum = 1;
        for (OrderRequest order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(order.getId());
            row.createCell(1).setCellValue(order.getRequestUser().getStore().getName());
            row.createCell(2).setCellValue(order.getRequestUser().getName());
            row.createCell(3).setCellValue(order.getRequestDate().format(formatter));
            row.createCell(4).setCellValue(order.getStatus().toString());
            row.createCell(5).setCellValue(order.getTotalPrice());
            row.createCell(6).setCellValue(
                    order.getRejectReason() != null ? order.getRejectReason() : "");
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 응답 헤더 설정 및 파일 출력
        String fileName = URLEncoder.encode(
                "발주내역리스트_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), "UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}