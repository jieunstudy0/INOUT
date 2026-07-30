package com.jstudy.inout.common.mail.dto;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
public class MailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String templateId;

    private String title;

    /** HTML 메일 본문 — varchar(255)로는 부족하므로 TEXT로 매핑 */
    @Column(columnDefinition = "TEXT")
    private String contents;

    private String sendEmail;

    private String sendUserName;

    private LocalDateTime regDate;
}
