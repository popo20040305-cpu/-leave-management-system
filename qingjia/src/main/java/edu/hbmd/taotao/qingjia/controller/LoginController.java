package edu.hbmd.taotao.qingjia.controller;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/login.html")
    public String login(){
        return "/login";
    }
    /**
     * 退出登录
     */
    @GetMapping("/logout.html")
    public String logout() {
        SecurityUtils.getSubject().logout();
        return "redirect:/login.html";
    }

}
