package edu.hbmd.taotao.qingjia.controller;

import edu.hbmd.taotao.qingjia.constant.UserTypeConstant;
import edu.hbmd.taotao.qingjia.service.MenuService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.vo.MenuVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class MainController {

    @Autowired
    MenuService menuService;

    @GetMapping("/")
    public String main() {
        return "/main/main";
    }

    @GetMapping("/top.html")
    public String top(Model model){
        Subject subject = SecurityUtils.getSubject();
        LoginUser loginUser = (LoginUser) subject.getPrincipal();
        String userName;
        if (loginUser.getType() == UserTypeConstant.USER_TYPE_STUDENT){
            userName = loginUser.getStudent().getName();
        }else{
            userName = loginUser.getTeacher().getName();
        }
        model.addAttribute("userName",userName);
        return "/main/top";
    }

    @GetMapping("/left.html")
    public String left(Model model){
        Subject subject = SecurityUtils.getSubject();
        LoginUser user = (LoginUser) subject.getPrincipal();
        Integer userId;
        if (user.getType() == UserTypeConstant.USER_TYPE_STUDENT){
            userId = user.getStudent().getId();
        }else{
            userId = user.getTeacher().getId();
        }
        List<MenuVo> menuVos = menuService.getMenuVosByUserIdAndUserType(userId,user.getType());
        model.addAttribute("menuVos",menuVos);
        return "/main/left";
    }
    @GetMapping("/index.html")
    public String index(){
        return "/main/index";
    }
}
