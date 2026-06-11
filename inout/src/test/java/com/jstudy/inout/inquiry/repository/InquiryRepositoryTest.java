package com.jstudy.inout.inquiry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.testsupport.InquiryJpaTestApplication;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = InquiryJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class InquiryRepositoryTest {

    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private TestEntityManager em;

    private User testUser1;
    private User testUser2;
    private Inquiry inquiry1; // user1 작성, 안 읽음
    private Inquiry inquiry2; // user1 작성, 읽음
    private Inquiry inquiry3; // user2 작성, 안 읽음

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .email("user1@test.com")
                .password("pwd1")
                .name("유저1")
                .phone("010-1111-1111")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        testUser2 = User.builder()
                .email("user2@test.com")
                .password("pwd2")
                .name("유저2")
                .phone("010-2222-2222")
                .birthday(LocalDate.of(1991, 2, 2))
                .build();
        em.persist(testUser1);
        em.persist(testUser2);

        inquiry1 = Inquiry.builder().title("질문1").content("내용1").author(testUser1).isRead(false).build();
        inquiry2 = Inquiry.builder().title("질문2").content("내용2").author(testUser1).isRead(true).build();
        inquiry3 = Inquiry.builder().title("질문3").content("내용3").author(testUser2).isRead(false).build();
        
        inquiryRepository.saveAll(List.of(inquiry1, inquiry2, inquiry3));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("읽지 않은 문의글(isRead=false) 개수를 정확히 카운트한다")
    void countByIsReadFalse() {
        long unreadCount = inquiryRepository.countByIsReadFalse();
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    @DisplayName("JOIN FETCH 쿼리: 전체 목록 페이징 조회 시 작성자(Author) 정보도 함께 가져온다")
    void findAllWithAuthor() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Inquiry> result = inquiryRepository.findAllWithAuthor(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().get(0).getAuthor().getName()).isNotNull(); 
    }

    @Test
    @DisplayName("JOIN FETCH 쿼리: 특정 작성자(userId)의 목록만 페이징 조회한다")
    void findAllWithAuthorByUserId() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Inquiry> result = inquiryRepository.findAllWithAuthorByUserId(testUser1.getId(), pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2); // testUser1이 쓴 글은 2개
        assertThat(result.getContent()).extracting(Inquiry::getTitle)
                .containsExactlyInAnyOrder("질문1", "질문2");
    }

    @Test
    @DisplayName("JOIN FETCH 쿼리: 문의글 단건 조회 시 작성자와 댓글 리스트를 함께 가져온다")
    void findByIdWithAuthorAndComments() {
        // when
        var foundInquiry = inquiryRepository.findByIdWithAuthorAndComments(inquiry1.getId());

        // then
        assertThat(foundInquiry).isPresent();
        assertThat(foundInquiry.get().getAuthor().getEmail()).isEqualTo("user1@test.com");
        assertThat(foundInquiry.get().getComments()).isNotNull();
    }
}