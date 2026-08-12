package com.jstudy.inout.inquiry.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jstudy.inout.inquiry.entity.InquiryComment;

public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {

    List<InquiryComment> findAllByInquiry_IdAndParentIsNullOrderByCreatedAtAsc(Long inquiryId);

    List<InquiryComment> findAllByParent_IdOrderByCreatedAtAsc(Long parentId);

    long countByInquiry_Id(Long inquiryId);

    void deleteAllByInquiry_Id(Long inquiryId);

    @Query("SELECT c FROM InquiryComment c " +
           "JOIN FETCH c.author " +
           "LEFT JOIN FETCH c.children ch " +
           "LEFT JOIN FETCH ch.author " +
           "WHERE c.inquiry.id = :inquiryId AND c.parent IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<InquiryComment> findCommentsWithAuthorByInquiryId(@Param("inquiryId") Long inquiryId);
}