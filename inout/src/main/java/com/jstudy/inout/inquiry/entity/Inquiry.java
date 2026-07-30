package com.jstudy.inout.inquiry.entity;

import java.util.List;
import java.util.ArrayList;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Getter
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Inquiry extends BaseTimeEntity { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(length = 255)
    private String originalFileName; 

    @Column(length = 255)
    private String savedFilePath;

    @Column(length = 50)
    private String aiCategory;

    @Column(columnDefinition = "TEXT")
    private String aiDraftAnswer;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InquiryComment> comments = new ArrayList<>();

    public void markAsRead() {
        this.isRead = true;
    }

    public void updateAiAnalysis(String category, String draftAnswer) {
        this.aiCategory = category;
        this.aiDraftAnswer = draftAnswer;
    }
}