package com.jstudy.inout.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminViewController {

    @GetMapping("/admin/orders")
    public String orders() {
        return "admin/order/index";
    }

    @GetMapping("/admin/stock")
    public String stock() {
        return "admin/stock/index";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }
}
