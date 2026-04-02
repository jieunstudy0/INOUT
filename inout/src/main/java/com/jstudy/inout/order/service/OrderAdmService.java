package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.order.dto.BulkOrderRequest;
import com.jstudy.inout.order.dto.BulkOrderResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.dto.OrderProcessRequest;
import com.jstudy.inout.order.dto.OrderProcessRequest.ItemStatusUpdate;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.exception.NotEnoughStockException;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

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

    @Transactional(readOnly = true)
    public List<OrderAdminResponse> getAllOrders(OrderStatus status) {

    	List<OrderRequest> orders;

        if (status == null) {
            orders = orderRequestRepository.findAllByOrderByRequestDateDesc();
        } else {
            orders = orderRequestRepository.findAllByStatusOrderByRequestDateDesc(status);
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

    @Transactional
    public void processOrderItems(Long orderId, OrderProcessRequest request, Long adminId) {
        OrderRequest order = orderRequestRepository.findById(orderId).orElseThrow();
        User adminUser = userRepository.findById(adminId).orElseThrow();
        
        boolean hasDelayedOrRejected = false;

        for (ItemStatusUpdate update : request.items()) {
            OrderDetail detail = orderDetailRepository.findById(update.orderDetailId()).orElseThrow();

            if (update.status().needsStockDeduction()) {
                approveItemStock(detail, adminUser, orderId); 
            }
            
            detail.updateStatus(update.status());

            if (!update.status().isProcessed()) {
                hasDelayedOrRejected = true;
            }
        }

        updateOrderStatus(order, hasDelayedOrRejected);        
        mailComponent.sendOrderStateEmail(order);
    }

    public BulkOrderResponse bulkApproveOrders(BulkOrderRequest request, Long adminId) {
        int successCount = 0;
        List<BulkOrderResponse.FailedOrder> failures = new ArrayList<>();

        for (Long orderId : request.getOrderIds()) {
            try {
            	
                processSingleOrderApproval(orderId, adminId);
                successCount++;
                
            } catch (NotEnoughStockException e) {

                processSingleOrderRejection(orderId, "자동 반려: " + e.getMessage());
                
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
                .failures(failures).build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderApproval(Long orderId, Long adminId) {
    	OrderRequest order = orderRequestRepository.findById(orderId)
    	        .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 없음", 404, "ADMIN_NOT_FOUND"));

        for (OrderDetail detail : order.getOrderDetails()) {

            if (!detail.getStatus().isWaiting()) {
                continue; 
            }
            approveItemStock(detail, adminUser, orderId); 
            detail.updateStatus(OrderDetailStatus.APPROVED);
        }

        order.updateStatus(OrderStatus.COMPLETED);
        order.updateProcessDate(LocalDateTime.now());
        
        mailComponent.sendOrderStateEmail(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderRejection(Long orderId, String reason) {
        OrderRequest order = orderRequestRepository.findById(orderId).orElseThrow();
        
        order.updateStatus(OrderStatus.REJECTED);
        order.updateRejectReason(reason);
        order.updateProcessDate(LocalDateTime.now());

        for (OrderDetail detail : order.getOrderDetails()) {
            detail.updateStatus(OrderDetailStatus.REJECTED);
        }     
        mailComponent.sendOrderStateEmail(order);
    }

    private void approveItemStock(OrderDetail detail, User adminUser, Long orderId) {

        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보 없음", 404, "ITEM_NOT_FOUND"));

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

    private void updateOrderStatus(OrderRequest order, boolean hasIssue) {
        if (hasIssue) order.updateStatus(OrderStatus.PARTIAL);
        else order.updateStatus(OrderStatus.COMPLETED);
        order.updateProcessDate(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public void exportOrdersToExcel(HttpServletResponse response) throws IOException {

        List<OrderRequest> orders = orderRequestRepository.findAllByOrderByRequestDateDesc();

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
            row.createCell(1).setCellValue(order.getRequestUser().getStore().getName());
            row.createCell(2).setCellValue(order.getRequestUser().getName());            
            String formattedDate = order.getRequestDate().format(formatter);
            row.createCell(3).setCellValue(formattedDate);           
            row.createCell(4).setCellValue(order.getStatus().toString());                     
            row.createCell(5).setCellValue(order.getTotalPrice()); 
            row.createCell(6).setCellValue(order.getRejectReason() != null ? order.getRejectReason() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = URLEncoder.encode("발주내역리스트_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), "UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    
    
}
