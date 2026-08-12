package com.jstudy.inout;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import com.jstudy.inout.common.mail.repository.MailTemplateRepository;

@SpringBootTest
class InoutApplicationTests {

	@MockBean
	private JavaMailSender javaMailSender;

	@MockBean
	private MailTemplateRepository mailTemplateRepository;

	@Test
	void contextLoads() {
	}

}
