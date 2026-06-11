package com.jstudy.inout.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.InquiryCreateRequest;
import com.jstudy.inout.inquiry.dto.InquiryDetailResponse;
import com.jstudy.inout.inquiry.dto.InquiryListResponse;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @InjectMocks
    private InquiryService inquiryService;

    @Mock private InquiryRepository inquiryRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("문의 목록 조회 - 관리자는 전체 목록을 조회한다")
    void getInquiryList_Admin() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().name("직원").build();
        Inquiry inquiry = Inquiry.builder().id(1L).title("문의").author(user).build();
        Page<Inquiry> pageResult = new PageImpl<>(List.of(inquiry), pageable, 1);

        given(inquiryRepository.findAllWithAuthor(pageable)).willReturn(pageResult);

        // when 
        Page<InquiryListResponse> result = inquiryService.getInquiryList(99L, true, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(inquiryRepository).findAllWithAuthor(pageable); // 관리자 전용 메서드 호출 확인
    }

    @Test
    @DisplayName("문의 목록 조회 - 직원은 본인 글만 조회한다")
    void getInquiryList_User() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inquiry> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(inquiryRepository.findAllWithAuthorByUserId(1L, pageable)).willReturn(emptyPage);

        // when 
        inquiryService.getInquiryList(1L, false, pageable);

        // then
        verify(inquiryRepository).findAllWithAuthorByUserId(1L, pageable); // 직원 전용 메서드 호출 확인
    }

    @Test
    @DisplayName("문의 상세 조회 - 관리자가 최초 조회하면 읽음(isRead) 처리된다")
    void getInquiryDetail_Admin_MarkAsRead() {
        // given
        User author = User.builder().id(1L).build();
        Inquiry inquiry = Inquiry.builder().id(100L).author(author).isRead(false).build(); 

        given(inquiryRepository.findById(100L)).willReturn(Optional.of(inquiry));

        // when 
        InquiryDetailResponse response = inquiryService.getInquiryDetail(100L, 99L, true);

        // then
        assertThat(inquiry.isRead()).isTrue(); 
    }

    @Test
    @DisplayName("문의 상세 조회 실패 - 직원이 다른 사람의 글을 조회하면 권한 에러 발생")
    void getInquiryDetail_Fail_Forbidden() {
        // given
        User author = User.builder().id(1L).build(); 
        Inquiry inquiry = Inquiry.builder().id(100L).author(author).build();

        given(inquiryRepository.findById(100L)).willReturn(Optional.of(inquiry));

        // when & then 
        assertThatThrownBy(() -> inquiryService.getInquiryDetail(100L, 2L, false))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("조회 권한이 없습니다.");
    }
}