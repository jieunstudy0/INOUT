package com.jstudy.inout.inquiry.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.entity.InquiryTargetType;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findAllByAuthor_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Inquiry> findAllByIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    java.util.List<Inquiry> findByAiDraftAnswerIsNullOrderByCreatedAtAsc(Pageable pageable);

    long countByIsReadFalse();

    long countByAiDraftAnswerIsNotNull();

    @Query("SELECT i FROM Inquiry i " +
           "JOIN FETCH i.author " +
           "LEFT JOIN FETCH i.comments " +
           "WHERE i.id = :inquiryId")
    java.util.Optional<Inquiry> findByIdWithAuthorAndComments(@Param("inquiryId") Long inquiryId);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.author ORDER BY i.createdAt DESC",
           countQuery = "SELECT count(i) FROM Inquiry i")
    Page<Inquiry> findAllWithAuthor(Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.author " +
                   "WHERE i.author.id = :userId ORDER BY i.createdAt DESC",
           countQuery = "SELECT count(i) FROM Inquiry i WHERE i.author.id = :userId")
    Page<Inquiry> findAllWithAuthorByUserId(@Param("userId") Long userId, Pageable pageable);

    // ── EMP: 본인이 본사로 작성한 문의 ──────────────────────────────────────────
    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.author " +
                   "WHERE i.author.id = :userId AND i.targetType = :targetType " +
                   "ORDER BY i.createdAt DESC",
           countQuery = "SELECT count(i) FROM Inquiry i " +
                        "WHERE i.author.id = :userId AND i.targetType = :targetType")
    Page<Inquiry> findByAuthorIdAndTargetType(
            @Param("userId") Long userId,
            @Param("targetType") InquiryTargetType targetType,
            Pageable pageable);

    // ── OWNER: 본인 매장의 직원이 점주에게 보낸 내부 문의 (targetType = OWNER) ──
    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.author a " +
                   "WHERE i.targetType = :targetType AND a.store.id = :storeId " +
                   "ORDER BY i.createdAt DESC",
           countQuery = "SELECT count(i) FROM Inquiry i JOIN i.author a " +
                        "WHERE i.targetType = :targetType AND a.store.id = :storeId")
    Page<Inquiry> findByTargetTypeAndStoreId(
            @Param("targetType") InquiryTargetType targetType,
            @Param("storeId") Long storeId,
            Pageable pageable);

    // ── ADMIN: writer가 ROLE_OWNER인 본사 문의 ──────────────────────────────────
    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.author a " +
                   "WHERE i.targetType = 'ADMIN' " +
                   "AND EXISTS (SELECT ur FROM UserRole ur JOIN ur.role r " +
                   "            WHERE ur.user = a AND r.roleName = :roleName) " +
                   "ORDER BY i.createdAt DESC",
           countQuery = "SELECT count(i) FROM Inquiry i JOIN i.author a " +
                        "WHERE i.targetType = 'ADMIN' " +
                        "AND EXISTS (SELECT ur FROM UserRole ur JOIN ur.role r " +
                        "            WHERE ur.user = a AND r.roleName = :roleName)")
    Page<Inquiry> findAdminInquiriesByWriterRole(
            @Param("roleName") String roleName,
            Pageable pageable);
}