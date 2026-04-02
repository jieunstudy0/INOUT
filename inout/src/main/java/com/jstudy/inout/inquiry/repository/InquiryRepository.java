package com.jstudy.inout.inquiry.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jstudy.inout.inquiry.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findAllByAuthor_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Inquiry> findAllByIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByIsReadFalse();

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
}