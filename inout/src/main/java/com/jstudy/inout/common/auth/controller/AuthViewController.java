package com.jstudy.inout.common.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

	@GetMapping("/user/login") 
    public String loginPage() {
        return "user/login"; 
    }
	
	
}
