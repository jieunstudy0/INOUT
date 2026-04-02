package com.jstudy.inout.common.mail.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.common.mail.dto.MailTemplate;


@Repository
public interface MailTemplateRepository extends JpaRepository<MailTemplate, Long> {

    Optional<MailTemplate> findByTemplateId(String templateId);

}
