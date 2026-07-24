package com.jstudy.inout.ai.service;

import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCsPersistenceService {

    private final InquiryRepository inquiryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateInquiryAnalysis(Long inquiryId, String category, String draftAnswer) {
        return inquiryRepository.findById(inquiryId)
                .map(inquiry -> {
                    inquiry.updateAiAnalysis(category, draftAnswer);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("[AI CS 자동화] inquiryId={} 저장 시점에 이미 삭제되었거나 존재하지 않음. 건너뜀.", inquiryId);
                    return false;
                });
    }
}
