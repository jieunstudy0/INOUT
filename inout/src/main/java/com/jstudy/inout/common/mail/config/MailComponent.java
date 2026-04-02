package com.jstudy.inout.common.mail.config;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class MailComponent {

    private final JavaMailSender javaMailSender;

    public boolean send(String fromEmail, String fromName, String toEmail, String toName, String title, String contents) {
  

        if (javaMailSender instanceof JavaMailSenderImpl impl) {
            log.info("메일 발송 시도 - Host: {}, Port: {}", impl.getHost(), impl.getPort());
        }

        boolean result = false;

        MimeMessagePreparator mimeMessagePreparator = mimeMessage -> { 
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            InternetAddress from = new InternetAddress(fromEmail, fromName, "UTF-8");
            InternetAddress to = new InternetAddress(toEmail, toName, "UTF-8");

            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(title);
            mimeMessageHelper.setText(contents, true);
        };

        try {
            javaMailSender.send(mimeMessagePreparator);
            result = true;
        } catch (Exception e) {
             log.error("메일 발송 실패: {}", e.getMessage());
             throw new RuntimeException("메일 발송에 실패했습니다.", e); 
        }

        return result;
    }
    
    @Async 
    public void sendOrderStateEmail(OrderRequest order) {
        try {
            User receiver = order.getRequestUser();
            String toEmail = receiver.getEmail();
            String toName = receiver.getName();
            
            String fromEmail = "jieunstudy@kakao.com"; 
            String fromName = "재고관리 시스템";
  
            String title = String.format("[발주알림] 주문번호 #%d 처리 결과 안내", order.getId());

            StringBuilder content = new StringBuilder();
            content.append("<h3>발주 처리 결과 안내</h3>");
            content.append("<p>안녕하세요, ").append(toName).append("님.</p>");
            content.append("<p>요청하신 발주 건(주문번호: <strong>").append(order.getId()).append("</strong>)의 상태가 변경되었습니다.</p>");
            content.append("<hr/>");
            content.append("<p><strong>처리 상태: </strong> <span style='color:blue;'>").append(order.getStatus()).append("</span></p>");
            
            if (order.getStatus() == OrderStatus.REJECTED && order.getRejectReason() != null) {
                content.append("<p><strong>반려 사유: </strong> <span style='color:red;'>").append(order.getRejectReason()).append("</span></p>");
            }
            content.append("<br/><p>시스템에 접속하여 상세 내역을 확인해주세요.</p>");

            this.send(fromEmail, fromName, toEmail, toName, title, content.toString());
            
            log.info("알림 메일 발송 성공 (비동기): OrderId={}", order.getId());

        } catch (Exception e) {
            log.error("알림 메일 발송 실패: {}", e.getMessage());
        }
    }
}