package com.jstudy.inout.common.mail;

import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.dto.MailTemplate;
import com.jstudy.inout.common.mail.repository.MailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MailTemplateCacheService {

    private final MailTemplateRepository mailTemplateRepository;

    @Cacheable(value = CacheConfig.MAIL_TEMPLATE, key = "#templateId")
    @Transactional(readOnly = true)
    public MailTemplate getMailTemplate(String templateId) {
        return mailTemplateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new InoutException("메일 템플릿이 존재하지 않습니다."));
    }
}
