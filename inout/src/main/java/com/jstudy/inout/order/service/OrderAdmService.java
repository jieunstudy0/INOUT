package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.util.UserDisplayNames;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.dto.*;
import com.jstudy.inout.order.dto.OrderProcessRequest.ItemStatusUpdate;
import com.jstudy.inout.order.entity.*;
import com.jstudy.inout.order.event.OrderStateChangedEvent;
import com.jstudy.inout.order.repository.*;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.service.DepositService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAdmService {

    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final OrderApprovalTxService orderApprovalTxService;
    private final DeliveryService deliveryService;
    private final DepositService depositService; 
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<OrderAdminResponse> getAllOrders(OrderStatus status) {
        List<OrderRequest> orders;
        if (status == null) {
            // 본사: 직원 기안(REQUESTED) 제외 — 점주 결제 완료(ORDERED) 이상만
            orders = orderRequestRepository.findAllHqVisibleWithDetailsOrderByDateDesc(OrderStatus.REQUESTED);
        } else if (status == OrderStatus.REQUESTED) {
            orders = List.of();
        } else {
            orders = orderRequestRepository.findAllWithDetailsByStatusOrderByDateDesc(status);
        }

        return orders.stream().map(order -> {
            String repItemName = "상품 없음";
            int itemCount = 0;
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                repItemName = order.getOrderDetails().get(0).getItem().getName();
                itemCount = order.getOrderDetails().size();
            }
            return OrderAdminResponse.builder()
                    .orderRequestId(order.getId())
                    .storeName(UserDisplayNames.storeName(order.getRequestUser()))
                    .employeeName(UserDisplayNames.displayName(order.getRequestUser()))
                    .requestDate(order.getRequestDate())
                    .status(order.getStatus())
                    .totalPrice(order.getTotalPrice())
                    .representativeItemName(repItemName)
                    .itemCount(itemCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderAdminDetailResponse getOrderDetail(Long orderId) {
        OrderRequest order = orderRequestRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        List<OrderAdminDetailResponse.ItemDto> items =
                (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
                .stream().map(d -> OrderAdminDetailResponse.ItemDto.builder()
                        .orderDetailId(d.getOrderDetailId())
                        .itemId(d.getItem().getItemId())
                        .itemName(d.getItem().getName())
                        .quantity(d.getRequestQuantity())
                        .priceSnapshot(d.getItemPriceSnapshot())
                        .subTotal(d.getItemPriceSnapshot() != null ? d.getItemPriceSnapshot() * d.getRequestQuantity() : 0L)
                        .status(d.getStatus())
                        .isAiSuggested(d.isAiSuggested())
                        .aiReason(d.getAiReason())
                        .build())
                .collect(Collectors.toList());

        return OrderAdminDetailResponse.builder()
                .orderRequestId(order.getId())
                .requestDate(order.getRequestDate())
                .status(order.getStatus())
                .storeName(UserDisplayNames.storeName(order.getRequestUser()))
                .employeeName(UserDisplayNames.displayName(order.getRequestUser()))
                .totalPrice(order.getTotalPrice())
                .rejectReason(order.getRejectReason())
                .items(items)
                .build();
    }

    @Transactional
    public void processOrderItems(Long orderId, OrderProcessRequest request, Long adminId) {
        OrderRequest order = orderRequestRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));

        if (order.getStatus() == OrderStatus.REQUESTED) {
            throw new InoutException("결제가 완료되지 않은 발주 건입니다.", 400, "NOT_PAID_ORDER");
        }
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InoutException("처리할 발주 상세 항목이 없습니다.", 400, "EMPTY_ORDER_ITEMS");
        }

        for (ItemStatusUpdate update : request.items()) {
            OrderDetail detail = orderDetailRepository
                    .findByOrderDetailIdAndOrderRequest_Id(update.orderDetailId(), orderId)
                    .orElseThrow(() -> new InoutException("해당 주문에 속한 발주 상세 항목을 찾을 수 없습니다.", 404, "ORDER_DETAIL_NOT_FOUND"));

            OrderDetailStatus current = detail.getStatus();
            OrderDetailStatus target = update.status();

            if (current == target) continue;

            if ((current == OrderDetailStatus.APPROVED || current == OrderDetailStatus.REJECTED)
                    && target != current) {
                throw new InoutException("이미 처리된 발주 상세 항목은 상태를 변경할 수 없습니다.", 400, "ALREADY_PROCESSED_DETAIL");
            }

            if (target == OrderDetailStatus.WAITING && current != OrderDetailStatus.WAITING) {
                throw new InoutException("대기 상태로 되돌릴 수 없습니다.", 400, "WAITING_ROLLBACK_FORBIDDEN");
            }

            if (current == OrderDetailStatus.APPROVED) {
                restoreItemStock(detail, adminUser, orderId);
            } else if (current == OrderDetailStatus.REJECTED) {
                reclaimRefundedDeposit(order, detail, adminId); 
            }

            if (target == OrderDetailStatus.APPROVED) {
                approveItemStock(detail, adminUser, orderId);
            } else if (target == OrderDetailStatus.REJECTED) {
                issuePartialRefund(order, detail, adminId);
            } else if (target == OrderDetailStatus.WAITING) {
                log.info("주문 상세 {}번 항목이 대기(WAITING) 상태로 복구되었습니다.", detail.getOrderDetailId());
            }
            detail.updateStatus(target);
        }

        updateOrderStatus(order);
        publishOrderStateChanged(order);
    }

    public BulkOrderResponse bulkApproveOrders(BulkOrderRequest request, Long adminId) {
        int successCount = 0;
        int autoRejectCount = 0;
        List<BulkOrderResponse.FailedOrder> failures = new ArrayList<>();

        for (Long orderId : request.getOrderIds()) {
            try {
                boolean approved = orderApprovalTxService.processSingleOrderApproval(orderId, adminId);
                if (approved) successCount++;
                else autoRejectCount++;
            } catch (NotEnoughStockException e) {
                orderApprovalTxService.processSingleOrderRejection(orderId, adminId, "자동 반려: " + e.getMessage());
                failures.add(BulkOrderResponse.FailedOrder.builder().orderId(orderId).reason("재고 부족").build());
            } catch (Exception e) {
                failures.add(BulkOrderResponse.FailedOrder.builder().orderId(orderId).reason("시스템 오류: " + e.getMessage()).build());
            }
        }

        return BulkOrderResponse.builder()
                .successCount(successCount).autoRejectCount(autoRejectCount)
                .failureCount(failures.size()).failures(failures).build();
    }

    /**
     * 본사 최종 승인 — ORDERED → APPROVED (재고 차감·배송 생성)
     */
    @Transactional(readOnly = true)
    public void approveOrder(Long orderId, Long adminId) {
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        if (!order.getStatus().isAwaitingHq()) {
            throw new InoutException("본사 승인 대기(ORDERED) 상태의 발주만 승인할 수 있습니다.", 400, "INVALID_ORDER_STATUS");
        }
        boolean ok = orderApprovalTxService.processSingleOrderApproval(orderId, adminId);
        if (!ok) {
            throw new InoutException("재고 부족으로 승인이 거부되었습니다. 발주가 자동 반려·환불 처리되었습니다.", 400, "STOCK_SHORTAGE");
        }
    }

    /**
     * 본사 반려 — ORDERED → REJECTED + 예치금 전액 환불
     */
    public void rejectOrder(Long orderId, Long adminId, OrderRejectRequest request) {
        OrderRequest peek = orderRequestRepository.findById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        if (!peek.getStatus().isAwaitingHq()) {
            throw new InoutException("본사 승인 대기(ORDERED) 상태의 발주만 반려할 수 있습니다.", 400, "INVALID_ORDER_STATUS");
        }
        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : "본사 발주 반려";
        orderApprovalTxService.processSingleOrderRejection(orderId, adminId, reason);
    }

    private void approveItemStock(OrderDetail detail, User adminUser, Long orderId) {
        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보 없음", 404, "ITEM_NOT_FOUND"));

        item.removeStock(detail.getRequestQuantity());

        StockUsageHistory usage = StockUsageHistory.builder()
                .item(item).user(adminUser).usageQuantity(detail.getRequestQuantity())
                .resultStock(item.getCurrentStock()).memo("발주 승인 차감 (#" + orderId + ")").build();
        usageHistoryRepository.save(usage);
    }

    private void restoreItemStock(OrderDetail detail, User adminUser, Long orderId) {
        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보 없음", 404, "ITEM_NOT_FOUND"));

        item.addStock(detail.getRequestQuantity());

        StockUsageHistory usage = StockUsageHistory.builder()
                .item(item).user(adminUser).usageQuantity(-detail.getRequestQuantity()) 
                .resultStock(item.getCurrentStock()).memo("발주 승인 취소/원상복구 (#" + orderId + ")").build();
        usageHistoryRepository.save(usage);
    }

    private void issuePartialRefund(OrderRequest order, OrderDetail detail, Long adminId) {
        long refundAmount = detail.getItemPriceSnapshot() * detail.getRequestQuantity();
        depositService.refundDeposit(
                order.getRequestUser().getId(), adminId, 
                DepositDto.RefundRequest.builder()
                        .amount(refundAmount)
                        .description("품목 반려 부분 환불 (#" + order.getId() + " - " + detail.getItem().getName() + ")")
                        .build()
        );
    }

    private void reclaimRefundedDeposit(OrderRequest order, OrderDetail detail, Long adminId) {
        long reclaimAmount = detail.getItemPriceSnapshot() * detail.getRequestQuantity();
               
        depositService.deductDeposit(
                order.getRequestUser().getId(), 
                adminId, 
                reclaimAmount, 
                "주문 반려 취소로 인한 예치금 재결제 (#" + order.getId() + ")"
        );
        log.info("환불된 예치금 재회수 완료. 금액: {}", reclaimAmount);
    }

    private void updateOrderStatus(OrderRequest order) {
        boolean allApproved = true;
        boolean allRejected = true;
        boolean hasWaitingOrDelayed = false;

        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getStatus() == OrderDetailStatus.WAITING || detail.getStatus() == OrderDetailStatus.DELAYED) {
                hasWaitingOrDelayed = true;
                allApproved = false;
                allRejected = false;
            } else if (detail.getStatus() == OrderDetailStatus.APPROVED) {
                allRejected = false;
            } else if (detail.getStatus() == OrderDetailStatus.REJECTED) {
                allApproved = false;
            }
        }

        if (allApproved) {
            order.updateStatus(OrderStatus.APPROVED);
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
        } else if (allRejected) {
            order.updateStatus(OrderStatus.REJECTED);
        } else {
            order.updateStatus(OrderStatus.PARTIAL);
            if (!hasWaitingOrDelayed) {
                deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
            }
        }
        order.updateProcessDate(LocalDateTime.now());
    }

    private void publishOrderStateChanged(OrderRequest order) {
        eventPublisher.publishEvent(new OrderStateChangedEvent(order.getId()));
    }

    @Transactional(readOnly = true)
    public void exportOrdersToExcel(HttpServletResponse response) throws IOException {
    	List<OrderRequest> orders = orderRequestRepository.findAllWithDetailsOrderByDateDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("발주 내역");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"주문번호", "매장명", "신청자", "신청일시", "상태", "총 금액", "반려 사유"};

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

        int rowNum = 1;
        for (OrderRequest order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(order.getId());
            row.createCell(1).setCellValue(UserDisplayNames.storeName(order.getRequestUser()));
            row.createCell(2).setCellValue(UserDisplayNames.displayName(order.getRequestUser()));
            row.createCell(3).setCellValue(order.getRequestDate().format(formatter));
            row.createCell(4).setCellValue(order.getStatus().toString());
            row.createCell(5).setCellValue(order.getTotalPrice());
            row.createCell(6).setCellValue(
                    order.getRejectReason() != null ? order.getRejectReason() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = URLEncoder.encode(
                "발주내역리스트_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), "UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}