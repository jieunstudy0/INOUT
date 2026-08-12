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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.entity.InquiryComment;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@Import(JpaAuditConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class InquiryCommentRepositoryTest {

    @Autowired private InquiryCommentRepository commentRepository;
    @Autowired private TestEntityManager em;

    private User testUser;
    private Inquiry testInquiry;
    private InquiryComment rootComment1;
    private InquiryComment rootComment2;
    private InquiryComment childComment;

    @BeforeEach
    void setUp() {
        em.getEntityManager().createNativeQuery("""
                CREATE TABLE IF NOT EXISTS "inquiries" (
                    "inquiry_id" BIGINT AUTO_INCREMENT PRIMARY KEY,
                    "title" VARCHAR(200) NOT NULL,
                    "content" CLOB NOT NULL,
                    "user_id" BIGINT NOT NULL,
                    "is_read" BOOLEAN NOT NULL,
                    "target_type" VARCHAR(10) NOT NULL,
                    "original_file_name" VARCHAR(255),
                    "saved_file_path" VARCHAR(255),
                    "ai_category" VARCHAR(50),
                    "ai_draft_answer" CLOB,
                    "created_at" TIMESTAMP,
                    "updated_at" TIMESTAMP
                )
                """).executeUpdate();
        em.getEntityManager().createNativeQuery("""
                CREATE TABLE IF NOT EXISTS "inquiry_comments" (
                    "comment_id" BIGINT AUTO_INCREMENT PRIMARY KEY,
                    "content" CLOB NOT NULL,
                    "inquiry_id" BIGINT NOT NULL,
                    "user_id" BIGINT NOT NULL,
                    "parent_id" BIGINT,
                    "created_at" TIMESTAMP,
                    "updated_at" TIMESTAMP
                )
                """).executeUpdate();

        testUser = User.builder()
                .email("commenter@test.com")
                .password("pwd")
                .name("댓글러")
                .phone("010-3333-3333")
                .birthday(LocalDate.of(1992, 3, 3))
                .build();
        em.persist(testUser);

        testInquiry = Inquiry.builder().title("테스트 문의").content("내용").author(testUser).isRead(false).build();
        em.persist(testInquiry);

        rootComment1 = InquiryComment.builder().inquiry(testInquiry).author(testUser).content("부모댓글1").parent(null).build();
        rootComment2 = InquiryComment.builder().inquiry(testInquiry).author(testUser).content("부모댓글2").parent(null).build();
        em.persist(rootComment1);
        em.persist(rootComment2);

        childComment = InquiryComment.builder().inquiry(testInquiry).author(testUser).content("대댓글1").parent(rootComment1).build();
        em.persist(childComment);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("최상위 댓글(Parent가 null)만 시간순으로 조회한다")
    void findAllByInquiry_IdAndParentIsNullOrderByCreatedAtAsc() {
        // when
        List<InquiryComment> rootComments = commentRepository
                .findAllByInquiry_IdAndParentIsNullOrderByCreatedAtAsc(testInquiry.getId());

        // then
        assertThat(rootComments).hasSize(2);
        assertThat(rootComments).extracting(InquiryComment::getContent)
                .containsExactly("부모댓글1", "부모댓글2");
    }

    @Test
    @DisplayName("특정 부모 댓글의 대댓글(자식)만 시간순으로 조회한다")
    void findAllByParent_IdOrderByCreatedAtAsc() {
        // when
        List<InquiryComment> childComments = commentRepository
                .findAllByParent_IdOrderByCreatedAtAsc(rootComment1.getId());

        // then
        assertThat(childComments).hasSize(1);
        assertThat(childComments.get(0).getContent()).isEqualTo("대댓글1");
    }

    @Test
    @DisplayName("복합 JOIN FETCH 쿼리: 부모 댓글과 연관된 대댓글, 그리고 각각의 작성자를 한 번에 조회한다")
    void findCommentsWithAuthorByInquiryId() {
        // when
        List<InquiryComment> comments = commentRepository.findCommentsWithAuthorByInquiryId(testInquiry.getId());

        // then
        assertThat(comments).hasSize(2);       
        InquiryComment fetchedRoot1 = comments.stream()
                .filter(c -> c.getContent().equals("부모댓글1"))
                .findFirst()
                .orElseThrow();
        
        assertThat(fetchedRoot1.getChildren()).hasSize(1);
        assertThat(fetchedRoot1.getChildren().get(0).getContent()).isEqualTo("대댓글1");

        assertThat(fetchedRoot1.getAuthor().getName()).isEqualTo("댓글러");
        assertThat(fetchedRoot1.getChildren().get(0).getAuthor().getName()).isEqualTo("댓글러");
    }

    @Test
    @DisplayName("특정 문의글에 달린 모든 댓글을 삭제한다")
    void deleteAllByInquiry_Id() {
        // when
        commentRepository.deleteAllByInquiry_Id(testInquiry.getId());
        em.flush();
        em.clear();

        // then
        long count = commentRepository.countByInquiry_Id(testInquiry.getId());
        assertThat(count).isZero();
    }
}